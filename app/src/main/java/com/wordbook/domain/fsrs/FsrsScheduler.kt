package com.wordbook.domain.fsrs

import com.wordbook.domain.fsrs.FsrsParameters.pyRound
import java.time.Duration
import java.time.Instant
import kotlin.math.E
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * FSRS-6 排程器。
 *
 * 本文件是 open-spaced-repetition/py-fsrs v6.3.2 中 Scheduler.review_card 的逐行 Kotlin 移植：
 * 官方的每一个分支（学习步 / 复习 / 重学步、短期记忆公式、难度均值回归、间隔模糊化 fuzz）
 * 都保持一致，因此可以与官方实现做逐值比对。
 * 期望值由 Python 参考实现生成，见 app/src/test 下的 FsrsGoldenTest。
 *
 * 权重来源见 [FsrsParameters]。
 */
class FsrsScheduler(
    parameters: DoubleArray = FsrsParameters.DEFAULT,
    val desiredRetention: Double = 0.9,
    /** 学习步（官方默认 1 分钟、10 分钟） */
    val learningSteps: List<Duration> = listOf(Duration.ofMinutes(1), Duration.ofMinutes(10)),
    /** 重学步（官方默认 10 分钟） */
    val relearningSteps: List<Duration> = listOf(Duration.ofMinutes(10)),
    val maximumInterval: Int = 36500,
    val enableFuzzing: Boolean = true,
    /** 注入随机源，便于测试时固定 */
    private val random: () -> Double = { Random.nextDouble() },
) {
    init {
        val errors = FsrsParameters.validate(parameters)
        require(errors.isEmpty()) { "FSRS 参数非法：" + errors.joinToString("; ") }
    }

    val parameters: DoubleArray = parameters.copyOf()

    /** 官方实现：_DECAY = -parameters[20]，_FACTOR = 0.9^(1/_DECAY) - 1 */
    private val decay: Double = -parameters[20]
    private val factor: Double = 0.9.pow(1.0 / decay) - 1.0

    // ------------------------------------------------------------------ 对外 API

    /** 卡片当前的可提取性（此刻能回忆起来的概率） */
    fun retrievability(card: FsrsCard, now: Instant = Instant.now()): Double =
        retrievabilityAt(card.stability, card.lastReview, now)

    /**
     * 复习一张卡，返回更新后的卡片与本次复习的间隔。
     * 逻辑与官方 review_card 完全一致。
     */
    fun review(card: FsrsCard, rating: Rating, reviewAt: Instant = Instant.now()): FsrsReviewResult {
        val stateBefore = card.state
        val stabilityBefore = card.stability
        val difficultyBefore = card.difficulty

        // 新卡等价于官方实现里刚创建的 Learning 卡（step = 0）
        var state = if (card.state == CardState.NEW) CardState.LEARNING else card.state
        var step: Int? = if (card.state == CardState.NEW) 0 else card.step
        var stability = card.stability
        var difficulty = card.difficulty
        val lastReview = card.lastReview

        val daysSinceLastReview: Long? =
            if (lastReview != null) elapsedDaysRaw(lastReview, reviewAt) else null

        var nextInterval: Duration

        when (state) {
            CardState.LEARNING -> {
                if (stability == null || difficulty == null) {
                    stability = initialStability(rating)
                    difficulty = initialDifficulty(rating, clamp = true)
                } else if (daysSinceLastReview != null && daysSinceLastReview < 1) {
                    stability = shortTermStability(stability, rating)
                    difficulty = nextDifficulty(difficulty, rating)
                } else {
                    stability = nextStability(
                        difficulty = difficulty,
                        stability = stability,
                        retrievability = retrievabilityAt(stability, lastReview, reviewAt),
                        rating = rating,
                    )
                    difficulty = nextDifficulty(difficulty, rating)
                }

                if (learningSteps.isEmpty() ||
                    ((step ?: 0) >= learningSteps.size && rating != Rating.AGAIN)
                ) {
                    state = CardState.REVIEW
                    step = null
                    nextInterval = Duration.ofDays(nextIntervalDays(stability).toLong())
                } else {
                    when (rating) {
                        Rating.AGAIN -> {
                            step = 0
                            nextInterval = learningSteps[0]
                        }
                        Rating.HARD -> {
                            val s = step ?: 0
                            nextInterval = when {
                                s == 0 && learningSteps.size == 1 ->
                                    learningSteps[0].multipliedBy(3).dividedBy(2)
                                s == 0 && learningSteps.size >= 2 ->
                                    learningSteps[0].plus(learningSteps[1]).dividedBy(2)
                                else -> learningSteps[s]
                            }
                        }
                        Rating.GOOD -> {
                            val s = step ?: 0
                            if (s + 1 == learningSteps.size) {
                                state = CardState.REVIEW
                                step = null
                                nextInterval = Duration.ofDays(nextIntervalDays(stability).toLong())
                            } else {
                                step = s + 1
                                nextInterval = learningSteps[s + 1]
                            }
                        }
                        Rating.EASY -> {
                            state = CardState.REVIEW
                            step = null
                            nextInterval = Duration.ofDays(nextIntervalDays(stability).toLong())
                        }
                    }
                }
            }

            CardState.REVIEW -> {
                val s = stability!!
                val d = difficulty!!
                stability = if (daysSinceLastReview != null && daysSinceLastReview < 1) {
                    shortTermStability(s, rating)
                } else {
                    nextStability(d, s, retrievabilityAt(s, lastReview, reviewAt), rating)
                }
                difficulty = nextDifficulty(d, rating)

                nextInterval = if (rating == Rating.AGAIN) {
                    if (relearningSteps.isEmpty()) {
                        Duration.ofDays(nextIntervalDays(stability).toLong())
                    } else {
                        state = CardState.RELEARNING
                        step = 0
                        relearningSteps[0]
                    }
                } else {
                    Duration.ofDays(nextIntervalDays(stability).toLong())
                }
            }

            CardState.RELEARNING -> {
                val s = stability!!
                val d = difficulty!!
                if (daysSinceLastReview != null && daysSinceLastReview < 1) {
                    stability = shortTermStability(s, rating)
                    difficulty = nextDifficulty(d, rating)
                } else {
                    stability = nextStability(d, s, retrievabilityAt(s, lastReview, reviewAt), rating)
                    difficulty = nextDifficulty(d, rating)
                }

                if (relearningSteps.isEmpty() ||
                    ((step ?: 0) >= relearningSteps.size && rating != Rating.AGAIN)
                ) {
                    state = CardState.REVIEW
                    step = null
                    nextInterval = Duration.ofDays(nextIntervalDays(stability).toLong())
                } else {
                    when (rating) {
                        Rating.AGAIN -> {
                            step = 0
                            nextInterval = relearningSteps[0]
                        }
                        Rating.HARD -> {
                            val s = step ?: 0
                            nextInterval = when {
                                s == 0 && relearningSteps.size == 1 ->
                                    relearningSteps[0].multipliedBy(3).dividedBy(2)
                                s == 0 && relearningSteps.size >= 2 ->
                                    relearningSteps[0].plus(relearningSteps[1]).dividedBy(2)
                                else -> relearningSteps[s]
                            }
                        }
                        Rating.GOOD -> {
                            val s = step ?: 0
                            if (s + 1 == relearningSteps.size) {
                                state = CardState.REVIEW
                                step = null
                                nextInterval = Duration.ofDays(nextIntervalDays(stability).toLong())
                            } else {
                                step = s + 1
                                nextInterval = relearningSteps[s + 1]
                            }
                        }
                        Rating.EASY -> {
                            state = CardState.REVIEW
                            step = null
                            nextInterval = Duration.ofDays(nextIntervalDays(stability).toLong())
                        }
                    }
                }
            }

            CardState.NEW -> error("NEW 状态已在前面转换为 LEARNING，不应到达此处")
        }

        if (enableFuzzing && state == CardState.REVIEW) {
            nextInterval = fuzzedInterval(nextInterval)
        }

        val updated = FsrsCard(
            cardId = card.cardId,
            state = state,
            step = step,
            stability = stability,
            difficulty = difficulty,
            due = reviewAt.plus(nextInterval),
            lastReview = reviewAt,
        )

        return FsrsReviewResult(
            card = updated,
            rating = rating,
            reviewedAt = reviewAt,
            stateBefore = stateBefore,
            stabilityBefore = stabilityBefore,
            difficultyBefore = difficultyBefore,
            intervalAfterMs = nextInterval.toMillis(),
            elapsedDays = daysSinceLastReview,
        )
    }

    /** 预估四档评分分别会把卡片排到多久之后（用于在学习页按钮上提示间隔），不修改传入的卡片 */
    fun previewIntervals(card: FsrsCard, now: Instant = Instant.now()): Map<Rating, Duration> =
        Rating.entries.associateWith { review(card, it, now).intervalAfterMs.let(Duration::ofMillis) }

    // -------------------------------------------------------------- 官方私有方法

    private fun initialStability(rating: Rating): Double =
        max(parameters[rating.value - 1], FsrsParameters.STABILITY_MIN)

    private fun initialDifficulty(rating: Rating, clamp: Boolean): Double {
        var difficulty = parameters[4] - E.pow(parameters[5] * (rating.value - 1)) + 1
        if (clamp) difficulty = clampDifficulty(difficulty)
        return difficulty
    }

    private fun nextIntervalDays(stability: Double): Int {
        var days = pyRound(stability / factor * (desiredRetention.pow(1.0 / decay) - 1))
        days = max(days, 1)
        return min(days, maximumInterval)
    }

    private fun shortTermStability(stability: Double, rating: Rating): Double {
        var increase = E.pow(parameters[17] * (rating.value - 3 + parameters[18])) *
            stability.pow(-parameters[19])
        if (rating != Rating.AGAIN) increase = max(increase, 1.0)
        return max(stability * increase, FsrsParameters.STABILITY_MIN)
    }

    private fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val arg1 = initialDifficulty(Rating.EASY, clamp = false)
        val deltaDifficulty = -(parameters[6] * (rating.value - 3))
        val arg2 = difficulty + (10.0 - difficulty) * deltaDifficulty / 9.0
        val next = parameters[7] * arg1 + (1 - parameters[7]) * arg2
        return clampDifficulty(next)
    }

    private fun nextStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating,
    ): Double {
        val next = if (rating == Rating.AGAIN) {
            nextForgetStability(difficulty, stability, retrievability)
        } else {
            nextRecallStability(difficulty, stability, retrievability, rating)
        }
        return max(next, FsrsParameters.STABILITY_MIN)
    }

    private fun nextForgetStability(difficulty: Double, stability: Double, retrievability: Double): Double {
        val longTerm = parameters[11] *
            difficulty.pow(-parameters[12]) *
            ((stability + 1).pow(parameters[13]) - 1) *
            E.pow((1 - retrievability) * parameters[14])
        val shortTerm = stability / E.pow(parameters[17] * parameters[18])
        return min(longTerm, shortTerm)
    }

    private fun nextRecallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating,
    ): Double {
        val hardPenalty = if (rating == Rating.HARD) parameters[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) parameters[16] else 1.0
        return stability * (
            1.0 +
                E.pow(parameters[8]) *
                (11 - difficulty) *
                stability.pow(-parameters[9]) *
                (E.pow((1 - retrievability) * parameters[10]) - 1) *
                hardPenalty *
                easyBonus
            )
    }

    private fun clampDifficulty(difficulty: Double): Double =
        min(max(difficulty, FsrsParameters.MIN_DIFFICULTY), FsrsParameters.MAX_DIFFICULTY)

    private fun fuzzedInterval(interval: Duration): Duration {
        val intervalDays = interval.toDays().toInt()
        if (intervalDays < 2.5) return interval

        var delta = 1.0
        FsrsParameters.FUZZ_RANGES.forEach { range ->
            delta += range.factor * max(min(intervalDays.toDouble(), range.end) - range.start, 0.0)
        }
        var minIvl = pyRound(intervalDays - delta)
        var maxIvl = pyRound(intervalDays + delta)
        minIvl = max(2, minIvl)
        maxIvl = min(maxIvl, maximumInterval)
        minIvl = min(minIvl, maxIvl)

        val fuzzed = random() * (maxIvl - minIvl + 1) + minIvl
        val fuzzedDays = min(pyRound(fuzzed), maximumInterval)
        return Duration.ofDays(fuzzedDays.toLong())
    }

    private fun retrievabilityAt(stability: Double?, lastReview: Instant?, now: Instant): Double {
        if (stability == null || lastReview == null) return 0.0
        return (1 + factor * elapsedDays(lastReview, now) / stability).pow(decay)
    }

    /** 官方实现固定使用整数天：max(0, (now - lastReview).days) */
    private fun elapsedDays(lastReview: Instant, now: Instant): Long =
        max(0L, elapsedDaysRaw(lastReview, now))

    private fun elapsedDaysRaw(lastReview: Instant, now: Instant): Long =
        Duration.between(lastReview, now).toDays()
}
