package com.wordbook.domain.fsrs

/**
 * FSRS 默认权重与边界值。
 *
 * 注意：这些数字来自 FSRS 官方实现，不是凭记忆编造的。
 *  - 源码：open-spaced-repetition/py-fsrs v6.3.2 的 fsrs/scheduler.py
 *    https://github.com/open-spaced-repetition/py-fsrs/blob/main/fsrs/scheduler.py
 *  - 取回日期：2026-10-06，原始文件留存在 tools/.cache/refs 下
 *  - DEFAULT_PARAMETERS / LOWER_BOUNDS_PARAMETERS / UPPER_BOUNDS_PARAMETERS / FSRS_DEFAULT_DECAY
 *    与官方实现逐项对应，索引一致。
 */
object FsrsParameters {

    /** FSRS-6 默认衰减参数（官方 FSRS_DEFAULT_DECAY） */
    const val DEFAULT_DECAY: Double = 0.1542

    /** 官方 DEFAULT_PARAMETERS，共 21 个（FSRS-6） */
    val DEFAULT: DoubleArray = doubleArrayOf(
        0.212,      // w0  初始 stability（重来）
        1.2931,     // w1  初始 stability（困难）
        2.3065,     // w2  初始 stability（良好）
        8.2956,     // w3  初始 stability（简单）
        6.4133,     // w4  初始 difficulty 基数
        0.8334,     // w5  初始 difficulty 斜率
        3.0194,     // w6  difficulty 变化率
        0.001,      // w7  difficulty 均值回归强度
        1.8722,     // w8  stability 增长基数
        0.1666,     // w9  stability 幂次衰减
        0.796,      // w10 可提取性影响
        1.4835,     // w11 遗忘后 stability 系数
        0.0614,     // w12 遗忘后 difficulty 幂次
        0.2629,     // w13 遗忘后 stability 幂次
        1.6483,     // w14 遗忘后可提取性影响
        0.6014,     // w15 困难惩罚
        1.8729,     // w16 简单奖励
        0.5425,     // w17 短期 stability 系数
        0.0912,     // w18 短期 stability 偏移
        0.0658,     // w19 短期 stability 幂次
        DEFAULT_DECAY, // w20 衰减（FSRS-6）
    )

    val LOWER_BOUNDS: DoubleArray = doubleArrayOf(
        0.001, 0.001, 0.001, 0.001, 1.0, 0.001, 0.001, 0.001, 0.0, 0.0,
        0.001, 0.001, 0.001, 0.001, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.1,
    )

    val UPPER_BOUNDS: DoubleArray = doubleArrayOf(
        100.0, 100.0, 100.0, 100.0, 10.0, 4.0, 4.0, 0.75, 4.5, 0.8,
        3.5, 5.0, 0.25, 0.9, 4.0, 1.0, 6.0, 2.0, 2.0, 0.8, 0.8,
    )

    const val MIN_DIFFICULTY: Double = 1.0
    const val MAX_DIFFICULTY: Double = 10.0
    const val STABILITY_MIN: Double = 0.001

    /** 官方 FUZZ_RANGES */
    val FUZZ_RANGES: List<FuzzRange> = listOf(
        FuzzRange(2.5, 7.0, 0.15),
        FuzzRange(7.0, 20.0, 0.10),
        FuzzRange(20.0, Double.POSITIVE_INFINITY, 0.05),
    )

    /** 校验权重数量与取值范围是否合法，返回错误列表（空表示合法） */
    fun validate(parameters: DoubleArray): List<String> {
        val errors = mutableListOf<String>()
        if (parameters.size != LOWER_BOUNDS.size) {
            errors += "参数个数应为 " + LOWER_BOUNDS.size + "，实际 " + parameters.size
            return errors
        }
        parameters.forEachIndexed { i, p ->
            if (p < LOWER_BOUNDS[i] || p > UPPER_BOUNDS[i]) {
                errors += "parameters[" + i + "] = " + p + " 超出范围 (" + LOWER_BOUNDS[i] + ", " + UPPER_BOUNDS[i] + ")"
            }
        }
        return errors
    }

    /** Python 的 round() 是四舍六入五取偶，与 Math.rint 行为一致 */
    fun pyRound(value: Double): Int = Math.rint(value).toInt()
}

data class FuzzRange(val start: Double, val end: Double, val factor: Double)
