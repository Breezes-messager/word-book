package com.wordbook.ui

import androidx.navigation.NavHostController

/**
 * 顶部 tab 之间的切换策略，统一走这里（底部导航、首页卡片、文章页的「去设置」都用它）。
 *
 * 之前这里用的是官方底部导航示例里的 `saveState = true` / `restoreState = true`，
 * 而首页那张「最近生成的文章」卡片是直接 `navigate("article")` 的，
 * 两者叠加会出现：从卡片进入文章页后再点底部「首页」没有任何反应
 * （restoreState 把旧的返回栈又恢复出来，界面依旧停在文章页）。
 *
 * 本 App 的 tab 页重建开销很小（各自有 ViewModel 和本地数据），
 * 所以不保存 tab 状态，统一用「回到首页栈底 + launchSingleTop」：
 * 返回栈永远是 [home] 或 [home, xxx]，行为可预测。
 * 回归测试见 app/src/test/.../TabNavigationTest.kt。
 */
internal fun NavHostController.switchTab(currentRoute: String?, route: String) {
    if (currentRoute == route) return
    navigate(route) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}
