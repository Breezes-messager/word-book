package com.wordbook.ui

import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * tab 切换策略的回归测试。
 *
 * 背景：用户反馈「首页点『最近生成的文章』进文章页后，再点底部『首页』没有反应」。
 * 原因是卡片用 `navigate("article")` 直接跳转，而底部导航用了
 * `saveState / restoreState`，两者叠加后返回栈状态错乱：
 * 点首页时 restoreState 又把旧的返回栈恢复出来，界面停在文章页。
 *
 * 这里用真实的 NavController + 同样的图结构复现那条路径，确保修好后不再回退。
 * 返回栈深度用 `popBackStack()` 的行为来断言（backQueue 是私有的）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TabNavigationTest {

    private fun newController(): NavHostController {
        val controller = TestNavHostController(ApplicationProvider.getApplicationContext())
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = controller.createGraph(startDestination = Routes.HOME) {
            composable(Routes.HOME) { }
            composable(Routes.ARTICLE) { }
            composable(Routes.STATS) { }
            composable(Routes.SETTINGS) { }
            composable(Routes.WORDS) { }
        }
        controller.setGraph(graph, null)
        return controller
    }

    private fun NavHostController.route(): String? = currentBackStackEntry?.destination?.route

    /** 用户报的那个 bug：卡片进文章页 → 点首页 */
    @Test
    fun `从首页卡片进入文章页后，点首页能回到首页`() {
        val nav = newController()

        // 首页卡片那条路径：直接 navigate，不带 popUpTo / saveState
        nav.navigate(Routes.ARTICLE)
        assertEquals(Routes.ARTICLE, nav.route())

        nav.switchTab(currentRoute = nav.route(), route = Routes.HOME)

        assertEquals("点首页之后必须真的回到首页", Routes.HOME, nav.route())
    }

    @Test
    fun `反复切换 tab 后返回键落在首页，说明栈里没有多余条目`() {
        val nav = newController()
        repeat(3) {
            nav.switchTab(nav.route(), Routes.ARTICLE)
            nav.switchTab(nav.route(), Routes.STATS)
            nav.switchTab(nav.route(), Routes.HOME)
        }
        nav.switchTab(nav.route(), Routes.STATS)
        assertEquals(Routes.STATS, nav.route())

        assertTrue("应该能退回到首页", nav.popBackStack())
        assertEquals(Routes.HOME, nav.route())
    }

    @Test
    fun `重复点击当前 tab 不会多压一层`() {
        val nav = newController()
        nav.switchTab(nav.route(), Routes.ARTICLE)
        nav.switchTab(nav.route(), Routes.ARTICLE)
        nav.switchTab(nav.route(), Routes.ARTICLE)
        assertEquals(Routes.ARTICLE, nav.route())

        assertTrue(nav.popBackStack())
        assertEquals("连点同一个 tab 不应该留下多余的层级", Routes.HOME, nav.route())
    }

    @Test
    fun `任意 tab 之间来回切换都落在正确的页面`() {
        val nav = newController()
        listOf(Routes.ARTICLE, Routes.STATS, Routes.SETTINGS, Routes.HOME, Routes.ARTICLE).forEach { target ->
            nav.switchTab(nav.route(), target)
            assertEquals(target, nav.route())
        }
    }

    @Test
    fun `从卡片进文章再点设置也正常`() {
        val nav = newController()
        nav.navigate(Routes.ARTICLE)
        nav.switchTab(nav.route(), Routes.SETTINGS)
        assertEquals(Routes.SETTINGS, nav.route())
        nav.switchTab(nav.route(), Routes.HOME)
        assertEquals(Routes.HOME, nav.route())
    }

    @Test
    fun `学习页这种压栈页面返回后仍然回到首页`() {
        val nav = newController()
        nav.navigate(Routes.WORDS)
        assertEquals(Routes.WORDS, nav.route())
        assertTrue(nav.popBackStack())
        assertEquals(Routes.HOME, nav.route())
        // 再从首页切 tab 也没问题
        nav.switchTab(nav.route(), Routes.STATS)
        assertEquals(Routes.STATS, nav.route())
    }
}
