package com.wordbook.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
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
import com.wordbook.ui.article.ArticleScreen
import com.wordbook.ui.home.HomeScreen
import com.wordbook.ui.settings.SettingsScreen
import com.wordbook.ui.stats.StatsScreen
import com.wordbook.ui.study.SessionMode
import com.wordbook.ui.study.StudyScreen
import com.wordbook.ui.words.WordListScreen

object Routes {
    const val HOME = "home"
    const val ARTICLE = "article"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val WORDS = "words"
    const val STUDY = "study"
    const val REVIEW = "review"
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
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
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
                    onOpenArticle = { navController.navigate(Routes.ARTICLE) },
                    onOpenWordList = { navController.navigate(Routes.WORDS) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                )
            }
            composable(Routes.ARTICLE) {
                ArticleScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.WORDS) { WordListScreen() }
            composable(Routes.STUDY) {
                StudyScreen(mode = SessionMode.NEW, onBack = { navController.popBackStack() })
            }
            composable(Routes.REVIEW) {
                StudyScreen(mode = SessionMode.REVIEW, onBack = { navController.popBackStack() })
            }
        }
    }
}
