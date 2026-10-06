package com.wordbook.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词表，数据来自 assets/words.db（由 tools/build_assets.py 生成后导入）。
 * id 由构建脚本按词书顺序分配，保持稳定。
 */
@Entity(
    tableName = "words",
    indices = [
        Index(value = ["headword"], unique = true),
        Index(value = ["deckPriority", "rank"]),
        Index(value = ["shuffleKey"]),
    ],
)
data class WordEntity(
    @PrimaryKey val id: Long,
    val headword: String,
    val phoneticUs: String?,
    val phoneticUk: String?,
    /** 中文释义，多行文本，例如 "n. 苹果\nv. 挑毛病" */
    val transCn: String?,
    /** 英英释义 */
    val transEn: String?,
    /** 例句 JSON：[{"en":"...","cn":"..."}] */
    val examplesJson: String?,
    /** 短语 JSON：[{"en":"...","cn":"..."}] */
    val phrasesJson: String?,
    /** 在所属词书中的序号 */
    val rank: Int,
    /** 词书文件名（KaoYan_2 / KaoYan_3 / KaoYanluan_1） */
    val deck: String,
    /** 词书优先级：0=KaoYan_2（主） 1=KaoYan_3 2=KaoYanluan_1 */
    val deckPriority: Int,
    /** 乱序模式下的排序键（构建时生成，保证多次启动顺序稳定） */
    val shuffleKey: Int,
)

/** 学习卡片，状态与 FSRS 参数一一对应。 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["wordId"], unique = true), Index(value = ["dueAt"])],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    /** 0=New 1=Learning 2=Review 3=Relearning */
    val state: Int = 0,
    /** 学习步 / 重学步下标 */
    val step: Int? = null,
    /** 到期时间（epoch millis） */
    val dueAt: Long,
    val stability: Double? = null,
    val difficulty: Double? = null,
    /** 复习总次数 */
    val reps: Int = 0,
    /** 遗忘（评“重来”）次数 */
    val lapses: Int = 0,
    val lastReviewAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** 完整复习日志：为将来用 FSRS Optimizer 重新拟合参数保留数据。 */
@Entity(
    tableName = "review_logs",
    indices = [Index(value = ["cardId"]), Index(value = ["dayKey"]), Index(value = ["reviewedAt"])],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val wordId: Long,
    /** 1=重来 2=困难 3=良好 4=简单 */
    val rating: Int,
    val reviewedAt: Long,
    /** 本地日期 yyyy-MM-dd，便于按天统计 */
    val dayKey: String,
    val stateBefore: Int,
    val stabilityBefore: Double?,
    val difficultyBefore: Double?,
    val stateAfter: Int,
    val stabilityAfter: Double?,
    val difficultyAfter: Double?,
    /** 复习后的间隔（毫秒） */
    val intervalAfterMs: Long,
    /** 距离上次复习的天数（首次学习为 -1） */
    val elapsedDays: Long,
    /** 用户在这张卡上停留的时间（毫秒） */
    val durationMs: Long,
)

/** 生成的文章（原始 JSON 落盘，断网时用缓存） */
@Entity(tableName = "articles", indices = [Index(value = ["dateKey"])])
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 本地日期 yyyy-MM-dd */
    val dateKey: String,
    val batchIndex: Int = 0,
    val title: String,
    val titleCn: String,
    /** 模型返回的原始 JSON 字符串 */
    val contentJson: String,
    val style: String,
    val targetWordCount: Int,
    val createdAt: Long,
)

/** 文章与目标词的关联，用于统计与点击查词 */
@Entity(
    tableName = "article_words",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["articleId"]), Index(value = ["wordId"])],
)
data class ArticleWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: Long,
    /** 词书里的词 id，模型给的目标词若不在词书中则为 null */
    val wordId: Long?,
    val target: String,
    val surface: String,
)
