package com.wordbook.data.repo

import com.wordbook.data.assets.AssetImporter
import com.wordbook.data.db.AppDatabase
import com.wordbook.data.db.DeckCount
import com.wordbook.data.db.WordEntity
import com.wordbook.data.prefs.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 词书仓库：数据全部来自本地 assets，离线可用 */
@Singleton
class WordRepository @Inject constructor(
    db: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val assetImporter: AssetImporter,
) {
    private val wordDao = db.wordDao()

    /** 首次启动导入词书 + 准备离线词典（幂等） */
    suspend fun ensureDataReady(onProgress: suspend (Int) -> Unit = {}) {
        assetImporter.importWordsIfNeeded(onProgress)
        assetImporter.ensureDictDatabase()
    }

    suspend fun totalCount(): Long = wordDao.count()

    suspend fun firstWords(limit: Int): List<WordEntity> = wordDao.firstWords(limit)

    suspend fun page(limit: Int, offset: Int): List<WordEntity> = wordDao.page(limit, offset)

    suspend fun deckStats(): List<DeckCount> = wordDao.countByDeck()

    suspend fun byIds(ids: List<Long>): List<WordEntity> = if (ids.isEmpty()) emptyList() else wordDao.byIds(ids)

    suspend fun byHeadword(headword: String): WordEntity? = wordDao.byHeadword(headword)

    suspend fun searchPrefix(prefix: String, limit: Int = 30): List<WordEntity> =
        wordDao.searchPrefix(prefix, limit)

    /** 取下一批新词，顺序由设置决定（顺序 / 乱序） */
    suspend fun takeNewWords(limit: Int): List<WordEntity> {
        if (limit <= 0) return emptyList()
        return if (settingsRepository.current().shuffleNewWords) {
            wordDao.newWordsShuffled(limit)
        } else {
            wordDao.newWordsSequential(limit)
        }
    }
}
