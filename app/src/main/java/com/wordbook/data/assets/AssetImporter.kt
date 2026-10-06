package com.wordbook.data.assets

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.withTransaction
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.WordEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首次启动时把 assets 里的两个数据库准备好：
 *  - words.db：词书，导入到 Room 业务库（Room 表结构与脚本生成的表结构一一对应）
 *  - dict.db ：离线词典，拷贝到应用私有目录后以只读方式打开
 *
 * assets 里的文件全部由 tools/build_assets.py 生成，不手工维护。
 */
@Singleton
class AssetImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) {
    companion object {
        private const val TAG = "AssetImporter"
        const val WORDS_ASSET = "words.db"
        const val DICT_ASSET = "dict.db"
        private const val BATCH = 500
    }

    /** 词书导入（幂等：已有数据则跳过） */
    suspend fun importWordsIfNeeded(onProgress: suspend (Int) -> Unit = {}): Int =
        withContext(Dispatchers.IO) {
            val existing = db.wordDao().count()
            if (existing > 0) return@withContext existing.toInt()

            val tmp = copyAssetToCache(WORDS_ASSET)
            val words = mutableListOf<WordEntity>()
            val sqlite = SQLiteDatabase.openDatabase(tmp.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            sqlite.use { dbFile ->
                dbFile.rawQuery(
                    """
                    SELECT id, headword, phoneticUs, phoneticUk, transCn, transEn,
                           examplesJson, phrasesJson, rank, deck, deckPriority, shuffleKey
                    FROM words ORDER BY id
                    """.trimIndent(),
                    null,
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        words += WordEntity(
                            id = cursor.getLong(0),
                            headword = cursor.getString(1),
                            phoneticUs = cursor.getStringOrNull(2),
                            phoneticUk = cursor.getStringOrNull(3),
                            transCn = cursor.getStringOrNull(4),
                            transEn = cursor.getStringOrNull(5),
                            examplesJson = cursor.getStringOrNull(6),
                            phrasesJson = cursor.getStringOrNull(7),
                            rank = cursor.getInt(8),
                            deck = cursor.getString(9),
                            deckPriority = cursor.getInt(10),
                            shuffleKey = cursor.getInt(11),
                        )
                    }
                }
            }
            tmp.delete()

            db.withTransaction {
                words.chunked(BATCH).forEachIndexed { index, chunk ->
                    db.wordDao().insertAll(chunk)
                    onProgress(((index + 1) * BATCH).coerceAtMost(words.size))
                }
            }
            Log.i(TAG, "词书导入完成，共 " + words.size + " 个词")
            words.size
        }

    /**
     * 离线词典：拷贝到 filesDir/dict/dict.db。assets 文件体积变化时自动重新拷贝，
     * 返回可只读打开的 File。
     */
    suspend fun ensureDictDatabase(): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "dict").apply { mkdirs() }
        val target = File(dir, "dict.db")
        val marker = File(dir, "dict.version")
        // build.gradle.kts 里对 "db" 设置了 noCompress，正常情况下 openFd 一定能拿到长度；
        // 万一拿不到（自定义打包方式），退化为“文件不存在时才拷贝”，不会每次启动都重复拷贝
        val assetSize = runCatching { context.assets.openFd(DICT_ASSET).use { it.length } }.getOrNull()
        val current = if (marker.exists()) marker.readText().trim().toLongOrNull() else null
        val needCopy = !target.exists() || (assetSize != null && current != assetSize)
        if (needCopy) {
            context.assets.open(DICT_ASSET).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output, 1 shl 16) }
            }
            marker.writeText((assetSize ?: target.length()).toString())
            Log.i(TAG, "离线词典已解压：" + target.absolutePath + " (" + target.length() + " bytes)")
        }
        target
    }

    private fun copyAssetToCache(name: String): File {
        val out = File(context.cacheDir, name)
        context.assets.open(name).use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output, 1 shl 16) }
        }
        return out
    }

    private fun android.database.Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)
}
