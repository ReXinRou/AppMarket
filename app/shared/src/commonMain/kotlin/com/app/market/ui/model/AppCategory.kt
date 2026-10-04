package com.app.market.ui.model

import com.app.market.domain.model.market.MarketAppInfo

/** Top-level catalogue sections exposed in the main navigation. */
enum class AppCategory(
    val seedKeyword: String,
) {
    GAMES("游戏"),
    APPS("应用"),
}

/**
 * Store APIs do not agree on a single category taxonomy. Keep the matching deliberately broad:
 * explicit game labels win, while apps are the complementary catalogue section.
 */
fun MarketAppInfo.matches(category: AppCategory): Boolean {
    val searchable = listOf(displayName, category, type).joinToString(" ").lowercase()
    val gameMarkers = listOf(
        "游戏", "网游", "手游", "休闲", "益智", "动作", "角色扮演", "策略", "射击", "棋牌",
        "模拟", "竞速", "冒险", "养成", "game", "games", "gaming", "taptap",
    )
    val isGame = gameMarkers.any(searchable::contains)
    return when (category) {
        AppCategory.GAMES -> isGame
        AppCategory.APPS -> !isGame
    }
}
