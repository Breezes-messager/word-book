package com.wordbook.domain.fsrs

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/**
 * 与 FSRS 官方 Python 实现（py-fsrs）的逐值比对测试。
 *
 * 黄金值由 tools/verify/generate_fsrs_golden.py 生成：
 *   python tools/verify/generate_fsrs_golden.py
 * 该脚本调用官方的 Scheduler（enable_fuzzing=False），把每个场景每一步的
 * state / step / stability / difficulty / 间隔写进 app/src/test/resources/fsrs_golden.json。
 */
class FsrsGoldenTest {

    @Serializable
    private data class GoldenFile(
        val generatedBy: String = "",
        val note: String = "",
        val scenarios: List<Scenario> = emptyList(),
    )

    @Serializable
    private data class Scenario(val name: String, val reviews: List<Review>)

    @Serializable
    private data class Review(
        val rating: Int,
        val elapsedMinutesFromPrevious: Long,
        val stateAfter: Int,
        val stepAfter: Int? = null,
        val stabilityAfter: Double,
        val difficultyAfter: Double,
        val intervalSeconds: Long,
    )

    private val base: Instant = Instant.parse("2026-01-01T09:00:00Z")

    private fun loadGolden(): GoldenFile {
        val stream = javaClass.getResourceAsStream("/fsrs_golden.json")
            ?: error("找不到 fsrs_golden.json，请先运行 python tools/verify/generate_fsrs_golden.py")
        val text = stream.bufferedReader().use { it.readText() }
        return Json { ignoreUnknownKeys = true }.decodeFromString(GoldenFile.serializer(), text)
    }

    @Test
    fun `与官方 py-fsrs 的排程结果逐值一致`() {
        val golden = loadGolden()
        assertTrue("黄金值文件为空，请重新生成", golden.scenarios.isNotEmpty())

        val scheduler = FsrsScheduler(enableFuzzing = false)
        golden.scenarios.forEach { scenario ->
            var card = FsrsCard(
                cardId = 1L,
                state = CardState.NEW,
                step = null,
                stability = null,
                difficulty = null,
                due = base,
                lastReview = null,
            )
            var now = base
            scenario.reviews.forEachIndexed { index, expected ->
                now = now.plus(Duration.ofMinutes(expected.elapsedMinutesFromPrevious))
                val result = scheduler.review(card, Rating.fromValue(expected.rating), now)
                card = result.card

                val where = scenario.name + " 第 " + (index + 1) + " 步"
                assertEquals("$where 状态不符", expected.stateAfter, card.state.value)
                assertEquals("$where 学习步不符", expected.stepAfter, card.step)
                assertClose("$where stability", expected.stabilityAfter, card.stability!!)
                assertClose("$where difficulty", expected.difficultyAfter, card.difficulty!!)
                assertEquals("$where 间隔不符", expected.intervalSeconds * 1000, result.intervalAfterMs)
                assertEquals(
                    "$where 到期时间应为复习时间 + 间隔",
                    now.plusMillis(result.intervalAfterMs),
                    card.due,
                )
            }
        }
    }

    /** 需求：新卡评“良好”后的首次间隔 */
    @Test
    fun `新卡评良好后进入第二个学习步`() {
        val scheduler = FsrsScheduler(enableFuzzing = false)
        val result = scheduler.review(newCard(), Rating.GOOD, base)

        assertEquals(CardState.LEARNING, result.card.state)
        assertEquals(1, result.card.step)
        assertEquals(Duration.ofMinutes(10).toMillis(), result.intervalAfterMs)
        assertClose("初始 stability 应为 w2", 2.3065, result.card.stability!!)
        assertEquals(base.plus(Duration.ofMinutes(10)), result.card.due)
    }

    /** 需求：评“重来”后重新入队（FSRS 学习步 = 1 分钟） */
    @Test
    fun `新卡评重来后一分钟内重新出现`() {
        val scheduler = FsrsScheduler(enableFuzzing = false)
        val result = scheduler.review(newCard(), Rating.AGAIN, base)

        assertEquals(CardState.LEARNING, result.card.state)
        assertEquals(0, result.card.step)
        assertEquals(Duration.ofMinutes(1).toMillis(), result.intervalAfterMs)
        assertClose("初始 stability 应为 w0", 0.212, result.card.stability!!)
        assertEquals(base.plus(Duration.ofMinutes(1)), result.card.due)
        assertTrue("重来后到期时间必须晚于当前时间", result.card.due.isAfter(base))
    }

    /** 需求：到期时间 = 复习时间 + 计算出的间隔，且间隔随 stability 增长 */
    @Test
    fun `复习卡间隔随 stability 单调增长`() {
        val scheduler = FsrsScheduler(enableFuzzing = false)
        var card = newCard()
        var now = base
        card = scheduler.review(card, Rating.GOOD, now).card
        now = now.plus(Duration.ofMinutes(10))
        card = scheduler.review(card, Rating.GOOD, now).card // 毕业进入 Review

        var lastInterval = 0L
        repeat(5) {
            now = card.due
            val result = scheduler.review(card, Rating.GOOD, now)
            card = result.card
            assertTrue(
                "复习间隔应递增：" + lastInterval + " -> " + result.intervalAfterMs,
                result.intervalAfterMs > lastInterval,
            )
            lastInterval = result.intervalAfterMs
            assertEquals(now.plusMillis(result.intervalAfterMs), card.due)
        }
    }

    /** 间隔模糊化必须落在官方定义的 fuzz 区间内 */
    @Test
    fun `间隔模糊化落在合理区间`() {
        val low = FsrsScheduler(enableFuzzing = true, random = { 0.0 })
        val high = FsrsScheduler(enableFuzzing = true, random = { 0.999999 })
        val exact = FsrsScheduler(enableFuzzing = false)

        var card = newCard()
        var now = base
        card = exact.review(card, Rating.EASY, now).card // 直接进入 Review，间隔较大
        now = card.due

        val expected = exact.review(card, Rating.GOOD, now).intervalAfterMs
        val lo = low.review(card, Rating.GOOD, now).intervalAfterMs
        val hi = high.review(card, Rating.GOOD, now).intervalAfterMs

        val fuzzLimit = expected * 0.25
        assertTrue("模糊后的间隔偏离过大（lo=" + lo + " expected=" + expected + "）", abs(lo - expected) <= fuzzLimit)
        assertTrue("模糊后的间隔偏离过大（hi=" + hi + " expected=" + expected + "）", abs(hi - expected) <= fuzzLimit)
        assertTrue("模糊化应该产生不同结果", lo != expected || hi != expected)
    }

    /** 目标保持率越高，间隔越短 */
    @Test
    fun `目标保持率影响间隔`() {
        var card = newCard()
        val s90 = FsrsScheduler(desiredRetention = 0.9, enableFuzzing = false)
        val s95 = FsrsScheduler(desiredRetention = 0.95, enableFuzzing = false)

        card = s90.review(card, Rating.EASY, base).card
        val at90 = s90.review(card, Rating.GOOD, card.due).intervalAfterMs
        val at95 = s95.review(card, Rating.GOOD, card.due).intervalAfterMs
        assertTrue("0.95 的间隔应短于 0.9（" + at95 + " vs " + at90 + "）", at95 < at90)
    }

    private fun newCard() = FsrsCard(
        cardId = 1L,
        state = CardState.NEW,
        step = null,
        stability = null,
        difficulty = null,
        due = base,
        lastReview = null,
    )

    private fun assertClose(where: String, expected: Double, actual: Double) {
        assertTrue(
            where + " 期望 " + expected + "，实际 " + actual,
            abs(expected - actual) < 1e-9 * maxOf(1.0, abs(expected)),
        )
    }
}
