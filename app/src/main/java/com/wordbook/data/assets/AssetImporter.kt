package com.wordbook.data.assets

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.withTransaction
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.WordEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首次启动时把 assets 里的两个数据库准备好：
 *  - words.db：词书，导入到 Room 业务库（表结构与脚本生成的表结构一一对应）
 *  - dict.db ：离线词典，拷贝到应用私有目录后以只读方式打开
 *
 * assets 里的文件全部由 tools/build_assets.py 生成，不手工维护。
 *
 * 词书升级（比如后来补了记忆法 / 同近义词字段）时，assets 文件体积会变，
 * 这里用体积做版本标记：体积变了就只更新词语详情，**不动 id、不动卡片**，
 * 所以老用户的复习进度不会丢，也不用重装。
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

    /** 后台刷新词书用（首页不再阻塞等它） */
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val columns = "id, headword, phoneticUs, phoneticUk, transCn, transEn, " +
        "examplesJson, phrasesJson, rank, deck, deckPriority, shuffleKey, " +
        "remMethod, synoJson, relWordJson"

    /**
     * 首页用：词书准备（幂等）。
     * - 空库 → 全量导入，**必须等**（没词什么都干不了）
     * - 已有词但 assets 变了 → **放后台刷新**，首页立刻可用
     *   （升级后不用再干等 1.8 秒，词条详情几秒后自己补齐）
     */
    suspend fun prepareWordBook(onProgress: suspend (Int) -> Unit = {}): Int =
        prepare(blockingRefresh = false, onProgress = onProgress)

    /** 导入备份前用：保证刷新也是同步完成的 */
    suspend fun importWordsIfNeeded(onProgress: suspend (Int) -> Unit = {}): Int =
        prepare(blockingRefresh = true, onProgress = onProgress)

    private suspend fun prepare(
        blockingRefresh: Boolean,
        onProgress: suspend (Int) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val marker = File(context.filesDir, "words.version")
        val assetSize = runCatching {
            context.assets.openFd(WORDS_ASSET).use { it.length }
        }.getOrNull()
        val exists = db.wordDao().count()
        // 注意：File.readText() 在文件不存在时会抛异常，必须先判断
        val knownVersion = if (marker.exists()) marker.readText().trim().toLongOrNull() else null

        when {
            exists == 0L -> {
                val count = fullImport(onProgress)
                if (assetSize != null) marker.writeText(assetSize.toString())
                count
            }

            assetSize != null && knownVersion != assetSize -> {
                // 先把标记写上，避免重复触发刷新
                marker.writeText(assetSize.toString())
                if (blockingRefresh) {
                    val count = refreshFromAsset(onProgress)
                    Log.i(TAG, "词书已升级，刷新了 " + count + " 条词的详情（复习进度未受影响）")
                } else {
                    backgroundScope.launch {
                        runCatching {
                            val count = refreshFromAsset({})
                            Log.i(TAG, "词书已在后台升级，刷新了 " + count + " 条词的详情")
                        }.onFailure { Log.w(TAG, "后台刷新词书失败", it) }
                    }
                }
                exists.toInt()
            }

            else -> exists.toInt()
        }
    }

    /** 全量导入（首次启动） */
    private suspend fun fullImport(onProgress: suspend (Int) -> Unit): Int {
        val words = readAssetWords()
        db.withTransaction {
            words.chunked(BATCH).forEachIndexed { index, chunk ->
                db.wordDao().insertAll(chunk)
                onProgress(((index + 1) * BATCH).coerceAtMost(words.size))
            }
        }
        Log.i(TAG, "词书导入完成，共 " + words.size + " 个词")
        return words.size
    }

    /**
     * 增量刷新：按 id 更新已有词条的详情字段，新增的词直接插入。
     * 因为构建脚本给 id 的分配是确定的（按词书顺序），所以已有卡片的 wordId 依然有效。
     */
    private suspend fun refreshFromAsset(onProgress: suspend (Int) -> Unit): Int {
        val words = readAssetWords()
        val existingIds = db.wordDao().allIds().toHashSet()
        db.withTransaction {
            words.forEachIndexed { index, word ->
                val dao = db.wordDao()
                if (existingIds.contains(word.id)) {
                    dao.updateDetails(
                        id = word.id,
                        headword = word.headword,
                        phoneticUs = word.phoneticUs,
                        phoneticUk = word.phoneticUk,
                        transCn = word.transCn,
                        transEn = word.transEn,
                        examplesJson = word.examplesJson,
                        phrasesJson = word.phrasesJson,
                        remMethod = word.remMethod,
                        synoJson = word.synoJson,
                        relWordJson = word.relWordJson,
                    )
                } else {
                    dao.insertAll(listOf(word))
                }
                if (index % BATCH == 0) onProgress(index)
            }
        }
        return words.size
    }

    private fun readAssetWords(): List<WordEntity> {
        val tmp = copyAssetToCache(WORDS_ASSET)
        val words = mutableListOf<WordEntity>()
        val sqlite = SQLiteDatabase.openDatabase(tmp.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        sqlite.use { file ->
            file.rawQuery("SELECT " + columns + " FROM words ORDER BY id", null).use { cursor ->
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
                        remMethod = cursor.getStringOrNull(12),
                        synoJson = cursor.getStringOrNull(13),
                        relWordJson = cursor.getStringOrNull(14),
                    )
                }
            }
        }
        tmp.delete()
        return words
    }

    /**
     * 离线词典：拷贝到 filesDir/dict/dict.db。assets 文件体积变化时自动重新拷贝，
     * 返回可只读打开的 File。
     */
    suspend fun ensureDictDatabase(): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "dict").apply { mkdirs() }
        val target = File(dir, "dict.db")
        val marker = File(dir, "dict.version")
        // build.gradle.kts 里对 "db" 设置了 noCompress，正常情况下 openFd 一定能拿到长度
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
