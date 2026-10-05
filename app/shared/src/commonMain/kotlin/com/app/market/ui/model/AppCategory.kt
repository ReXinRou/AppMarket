package com.app.market.ui.model

import com.app.market.domain.model.market.AppKind
import com.app.market.domain.model.market.MarketAppInfo

/** Top-level catalogue sections exposed in the main navigation. */
enum class AppCategory(
    val seedKeyword: String,
    val kind: AppKind,
) {
    GAMES("游戏", AppKind.GAME),
    APPS("应用", AppKind.APP),
}

/**
 * A section shows only items whose source-declared [AppKind] matches it. Items the source could not
 * classify (UNKNOWN) belong to neither section — guessing from the display name is what put games in
 * the apps list before, so it is deliberately not used here.
 */
fun MarketAppInfo.matches(category: AppCategory): Boolean = kind == category.kind
