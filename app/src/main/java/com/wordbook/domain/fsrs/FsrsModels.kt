package com.wordbook.domain.fsrs

import java.time.Instant

/**
 * 四档评分，取值与 FSRS 官方实现保持一致（1=重来 2=困难 3=良好 4=简单）。
 * 参考：https://github.com/open-spaced-repetition/py-fsrs/blob/main/fsrs/rating.py
 */
enum class Rating(val value: Int) {
    AGAIN(1), HARD(2), GOOD(3), EASY(4);

    companion object {
        fun fromValue(v: Int): Rating = entries.first { it.value == v }
    }
}

/**
 * 卡片状态。
 * NEW 是本 App 自行增加的“未学习”状态，进入 FSRS 时等价于官方实现里
 * 刚创建的 Learning 卡（step = 0，stability/difficulty = null）。
 * 其余取值与官方实现一致：Learning=1, Review=2, Relearning=3。
 */
enum class CardState(val value: Int) {
    NEW(0), LEARNING(1), REVIEW(2), RELEARNING(3);

    companion object {
        fun fromValue(v: Int): CardState = entries.first { it.value == v }
    }
}

/** FSRS 排程所需的最小卡片信息（与持久化解耦，便于纯 JVM 单元测试）。 */
data class FsrsCard(
    val cardId: Long,
    val state: CardState,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val due: Instant,
    val lastReview: Instant?,
)

/** 一次复习的结果：新卡片状态 + 本次排程的间隔。 */
data class FsrsReviewResult(
    val card: FsrsCard,
    val rating: Rating,
    val reviewedAt: Instant,
    val stateBefore: CardState,
    val stabilityBefore: Double?,
    val difficultyBefore: Double?,
    /** 本次评分后计算出的下一次间隔（毫秒） */
    val intervalAfterMs: Long,
    /** 距离上次复习的天数（首次学习为 null） */
    val elapsedDays: Long?,
)
