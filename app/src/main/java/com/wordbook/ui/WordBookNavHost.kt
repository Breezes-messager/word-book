package com.wordbook.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.wordbook.ui.article.ArticleReaderScreen
import com.wordbook.ui.article.ArticleScreen
import com.wordbook.ui.home.HomeScreen
import com.wordbook.ui.settings.SettingsScreen
import com.wordbook.ui.stats.StatsScreen
import com.wordbook.ui.study.SessionMode
import com.wordbook.ui.study.StudyScreen
import com.wordbook.ui.search.SearchScreen

object Routes {
    const val HOME = "home"
    const val ARTICLE = "article"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val STUDY = "study"
    const val REVIEW = "review"

    /** 历史文章阅读器，带文章 id */
    const val ARTICLE_READER = "reader/{articleId}"

    fun articleReader(id: Long) = "reader/" + id
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "首页", Icons.Default.Home),
    Tab(Routes.ARTICLE, "文章", Icons.AutoMirrored.Filled.List),
    Tab(Routes.STATS, "统计", Icons.Default.DateRange),
    Tab(Routes.SETTINGS, "设置", Icons.Default.Settings),
)

@Composable
fun WordBookNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(currentRoute, tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartStudy = { navController.navigate(Routes.STUDY) },
                    onStartReview = { navController.navigate(Routes.REVIEW) },
                    // 和底部导航用同一套跳转逻辑，避免返回栈里堆出重复的 article
                    onOpenArticle = { navController.switchTab(currentRoute, Routes.ARTICLE) },
                    onOpenStats = { navController.switchTab(currentRoute, Routes.STATS) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                )
            }
            composable(Routes.ARTICLE) {
                ArticleScreen(
                    onOpenSettings = { navController.switchTab(currentRoute, Routes.SETTINGS) },
                    onOpenReader = { id -> navController.navigate(Routes.articleReader(id)) },
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    onOpenReader = { id -> navController.navigate(Routes.articleReader(id)) },
                )
            }
            composable(
                route = Routes.ARTICLE_READER,
                arguments = listOf(navArgument("articleId") { type = NavType.LongType }),
            ) { entry ->
                ArticleReaderScreen(
                    articleId = entry.arguments?.getLong("articleId") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.SEARCH) {
                SearchScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.STUDY) {
                StudyScreen(mode = SessionMode.NEW, onBack = { navController.popBackStack() })
            }
            composable(Routes.REVIEW) {
                StudyScreen(mode = SessionMode.REVIEW, onBack = { navController.popBackStack() })
            }
        }
    }
}
