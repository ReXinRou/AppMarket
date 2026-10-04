package com.app.market.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.market.domain.model.market.MarketAppInfo
import com.app.market.domain.model.market.AppSource
import com.app.market.resources.Res
import com.app.market.resources.nav_apps
import com.app.market.resources.nav_games
import com.app.market.resources.search_apps_hint
import com.app.market.resources.search_games_hint
import com.app.market.ui.model.AppCategory
import com.app.market.viewmodel.SearchViewModel

/**
 * A catalogue section, not the global search page.
 *
 * Each section starts from its own catalogue seed, keeps only items belonging to that section,
 * and lets the user refine the current section with the search field.
 */
@Composable
fun CategoryTab(
    category: AppCategory,
    viewModel: SearchViewModel,
    bottomPadding: Dp,
    onOpenDetail: (MarketAppInfo) -> Unit,
    isCurrentPage: Boolean,
    sources: kotlinx.coroutines.flow.StateFlow<Set<AppSource>>,
) {
    val selectedSources by sources.collectAsStateWithLifecycle()
    SearchTab(
        viewModel = viewModel,
        bottomPadding = bottomPadding,
        onOpenDetail = onOpenDetail,
        isCurrentPage = isCurrentPage,
        category = category,
        categorySources = selectedSources,
        titleRes = when (category) {
            AppCategory.GAMES -> Res.string.nav_games
            AppCategory.APPS -> Res.string.nav_apps
        },
        searchHintRes = when (category) {
            AppCategory.GAMES -> Res.string.search_games_hint
            AppCategory.APPS -> Res.string.search_apps_hint
        },
    )
}
