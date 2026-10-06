package com.wordbook.domain.lemmatize

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 词形还原 / 文本清洗的单元测试（完全离线，不依赖 Android） */
class WordNormalizerTest {

    @Test
    fun `去掉句尾标点和引号并转小写`() {
        assertEquals("running", WordNormalizer.normalize("Running."))
        assertEquals("hello", WordNormalizer.normalize("\"Hello!\""))
        assertEquals("word", WordNormalizer.normalize("(word),"))
        assertEquals("apple", WordNormalizer.normalize("  Apple  "))
    }

    @Test
    fun `处理所有格`() {
        assertEquals("mom's", WordNormalizer.normalize("Mom's"))
        val candidates = WordNormalizer.candidates("Mom's,")
        assertEquals("mom's", candidates.first())
        assertTrue("应该能去掉所有格", candidates.contains("mom"))
    }

    @Test
    fun `处理常见缩写`() {
        val dont = WordNormalizer.candidates("don't")
        assertTrue(dont.contains("do"))
        val its = WordNormalizer.candidates("it's")
        assertTrue(its.contains("it"))
        val weve = WordNormalizer.candidates("we've")
        assertTrue(weve.contains("we"))
    }

    @Test
    fun `处理连字符`() {
        val candidates = WordNormalizer.candidates("well-known")
        assertTrue(candidates.contains("well known"))
        assertTrue(candidates.contains("well"))
    }

    @Test
    fun `保留撇号与内部连字符`() {
        assertEquals("o'clock", WordNormalizer.normalize("O'clock"))
        assertEquals("state-of-the-art", WordNormalizer.normalize("state-of-the-art."))
    }

    @Test
    fun `空输入与纯标点返回空`() {
        assertEquals("", WordNormalizer.normalize("..."))
        assertTrue(WordNormalizer.candidates("!!!").isEmpty())
    }

    @Test
    fun `可点单词判断`() {
        assertTrue(WordNormalizer.isLookupable("Running"))
        assertTrue(WordNormalizer.isLookupable("well-known"))
        assertTrue(!WordNormalizer.isLookupable("123"))
        assertTrue(!WordNormalizer.isLookupable("—"))
    }
}
