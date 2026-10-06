package com.wordbook.domain.article

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticlePlanTest {

    @Test
    fun `词数不足 5 个不生成`() {
        assertTrue(ArticlePlan.batchSizes(0).isEmpty())
        assertTrue(ArticlePlan.batchSizes(4).isEmpty())
    }

    @Test
    fun `5 到 20 个词生成一篇`() {
        assertEquals(listOf(5), ArticlePlan.batchSizes(5))
        assertEquals(listOf(12), ArticlePlan.batchSizes(12))
        assertEquals(listOf(20), ArticlePlan.batchSizes(20))
    }

    @Test
    fun `超过 20 个词自动分篇且每篇不超过 20`() {
        listOf(21, 25, 40, 41, 60, 77).forEach { total ->
            val sizes = ArticlePlan.batchSizes(total)
            assertEquals("总数应等于 " + total, total, sizes.sum())
            assertTrue("每篇不超过 20： " + sizes, sizes.all { it <= ArticlePlan.MAX_PER_ARTICLE })
            assertTrue("每篇至少 5 个： " + sizes, sizes.all { it >= 5 })
        }
    }

    @Test
    fun `21 个词拆成两篇`() {
        assertEquals(listOf(11, 10), ArticlePlan.batchSizes(21))
    }

    @Test
    fun `41 个词拆成三篇`() {
        assertEquals(listOf(14, 14, 13), ArticlePlan.batchSizes(41))
    }
}
