package com.wordbook.data.dict

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.wordbook.data.assets.AssetImporter
import com.wordbook.domain.lemmatize.WordNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 词典里的一条记录（来自 ECDICT 裁剪版） */
data class DictEntry(
    val word: String,
    val phonetic: String?,
    val translation: String?,
    val definition: String?,
    val pos: String?,
    val tag: String?,
    val collins: Int,
    val oxford: Int,
    val exchange: String?,
)

/** 查词结果 */
sealed interface DictLookupResult {
    /** 精确命中 */
    data class Exact(val entry: DictEntry, val query: String) : DictLookupResult
    /** 通过词形还原命中：running → run */
    data class Lemmatized(val entry: DictEntry, val query: String, val lemma: String) : DictLookupResult
    /** 前缀模糊匹配，给出候选 */
    data class Suggestions(val query: String, val entries: List<DictEntry>) : DictLookupResult
    /** 词典未收录 */
    data class NotFound(val query: String, val tried: List<String>) : DictLookupResult
}

/**
 * 离线词典仓库：只读打开 app/src/main/assets/dict.db（首次启动拷贝到私有目录）。
 * 查词顺序：精确匹配 → ECDICT exchange 词形还原 → 前缀模糊候选 → 未收录。
 * 全程不联网、不调用大模型。
 */
@Singleton
class DictRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetImporter: AssetImporter,
) {
    companion object {
        private const val TAG = "DictRepository"
        private const val CACHE_SIZE = 512
    }

    @Volatile
    private var database: SQLiteDatabase? = null
    private val openMutex = Mutex()

    /** 简单的 LRU：同一篇文章反复点同一个词可以立刻返回 */
    private val cache = object : LinkedHashMap<String, DictLookupResult>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DictLookupResult>?): Boolean =
            size > CACHE_SIZE
    }

    private suspend fun db(): SQLiteDatabase {
        database?.let { if (it.isOpen) return it }
        return openMutex.withLock {
            database?.let { if (it.isOpen) return@withLock it }
            val file = assetImporter.ensureDictDatabase()
            val opened = SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            database = opened
            opened
        }
    }

    /** 查词主入口（IO 线程执行，命中缓存时几乎零开销） */
    suspend fun lookup(raw: String): DictLookupResult = withContext(Dispatchers.IO) {
        val key = WordNormalizer.normalize(raw)
        if (key.isEmpty()) return@withContext DictLookupResult.NotFound(raw, emptyList())
        synchronized(cache) { cache[key] }?.let { return@withContext it }

        val sqlite = db()
        val tried = mutableListOf<String>()
        var result: DictLookupResult? = null

        for (candidate in WordNormalizer.candidates(raw)) {
            tried += candidate
            val exact = queryExact(sqlite, candidate)
            if (exact != null) {
                result = DictLookupResult.Exact(exact, key)
                break
            }
            val lemma = queryLemma(sqlite, candidate)
            if (lemma != null) {
                val entry = queryExact(sqlite, lemma)
                if (entry != null) {
                    result = DictLookupResult.Lemmatized(entry, key, lemma)
                    break
                }
                // 词形还原表里有，但词条本身没收录：仍然把原形告诉调用方
                result = DictLookupResult.Lemmatized(
                    DictEntry(lemma, null, null, null, null, null, 0, 0, null),
                    key,
                    lemma,
                )
                break
            }
        }

        if (result == null) {
            val suggestions = queryPrefix(sqlite, key, 20)
            result = if (suggestions.isNotEmpty()) {
                DictLookupResult.Suggestions(key, suggestions)
            } else {
                // 用去尾字母的宽松前缀再试一次（例如 running 拼错成 runing）
                val loose = if (key.length > 3) queryPrefix(sqlite, key.dropLast(1), 20) else emptyList()
                if (loose.isNotEmpty()) DictLookupResult.Suggestions(key, loose)
                else DictLookupResult.NotFound(key, tried.distinct())
            }
        }

        synchronized(cache) { cache[key] = result }
        result
    }

    private fun queryExact(sqlite: SQLiteDatabase, word: String): DictEntry? =
        sqlite.rawQuery(
            "SELECT word, phonetic, translation, definition, pos, tag, collins, oxford, exchange" +
                " FROM dict WHERE word = ? COLLATE NOCASE LIMIT 1",
            arrayOf(word),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toEntry() else null }

    private fun queryLemma(sqlite: SQLiteDatabase, surface: String): String? =
        sqlite.rawQuery(
            "SELECT lemma FROM lemma WHERE surface = ? COLLATE NOCASE LIMIT 1",
            arrayOf(surface),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private fun queryPrefix(sqlite: SQLiteDatabase, prefix: String, limit: Int): List<DictEntry> {
        val escaped = prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return sqlite.rawQuery(
            "SELECT word, phonetic, translation, definition, pos, tag, collins, oxford, exchange" +
                " FROM dict WHERE word LIKE ? ESCAPE '\\' ORDER BY frq DESC, word ASC LIMIT ?",
            arrayOf(escaped + "%", limit.toString()),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toEntry()) }
        }
    }

    private fun android.database.Cursor.toEntry(): DictEntry = DictEntry(
        word = getString(0) ?: "",
        phonetic = getString(1),
        translation = getString(2),
        definition = getString(3),
        pos = getString(4),
        tag = getString(5),
        collins = getInt(6),
        oxford = getInt(7),
        exchange = getString(8),
    )

    /** 词典里收录了多少词（设置页 / 统计页显示用） */
    suspend fun size(): Int = withContext(Dispatchers.IO) {
        runCatching {
            db().rawQuery("SELECT COUNT(*) FROM dict", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }.onFailure { Log.w(TAG, "读取词典条数失败", it) }.getOrDefault(0)
    }
}
