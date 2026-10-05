package com.app.market.viewmodel

import com.app.market.domain.model.download.DownloadMeta
import com.app.market.domain.model.download.DownloadState
import com.app.market.domain.model.download.DownloadTaskKey
import com.app.market.domain.model.install.DeltaFallback
import com.app.market.domain.model.install.InstallUserAction
import com.app.market.domain.model.installed.PackageChange
import com.app.market.domain.model.market.AppComments
import com.app.market.domain.model.market.AppDetail
import com.app.market.domain.model.market.AppKind
import com.app.market.domain.model.market.AppSource
import com.app.market.domain.model.market.MarketAppInfo
import com.app.market.domain.model.market.SearchPage
import com.app.market.domain.model.preference.HomePage
import com.app.market.domain.model.today.TodayArticle
import com.app.market.domain.model.today.TodayFeedPage
import com.app.market.domain.model.update.IgnoredUpdate
import com.app.market.domain.model.update.ManualUpdateRequest
import com.app.market.domain.model.update.ManualUpdateResult
import com.app.market.domain.repository.DownloadRepository
import com.app.market.domain.repository.MarketSourceRepository
import com.app.market.domain.repository.PackageRepository
import com.app.market.domain.repository.SearchHistoryRepository
import com.app.market.domain.repository.UpdatePreferencesRepository
import com.app.market.platform.ImageSaveResult
import com.app.market.platform.UiPlatform
import com.app.market.ui.model.AppCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Section pagination must keep pulling pages when a page contains only foreign-kind items; stopping
 * there is what made scrolling to the bottom of 游戏/应用 look like "load more no longer works".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelCategoryPagingTest {
    @Test
    fun loadMoreKeepsPullingWhilePagesAreForeignKind() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source = StubSearchSources(
                pages = listOf(
                    SearchPage(
                        items = listOf(game("g1"), app("a1"), game("g2")),
                        hasMore = true,
                    ),
                    SearchPage(
                        items = listOf(app("a2"), app("a3"), game("g3")),
                        hasMore = true,
                    ),
                    SearchPage(items = listOf(game("g4")), hasMore = false),
                ),
            )
            val viewModel = viewModel(source)
            advanceUntilIdle()

            viewModel.searchWith("游戏", setOf(AppSource.HONOR), AppCategory.GAMES)
            advanceUntilIdle()
            assertEquals(listOf("g1", "g2"), viewModel.uiState.value.results.map { it.app.packageName })

            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(
                listOf("g1", "g2", "g3"),
                viewModel.uiState.value.results.map { it.app.packageName },
                "a foreign-kind page should not stop the section from loading further",
            )

            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(listOf("g1", "g2", "g3", "g4"), viewModel.uiState.value.results.map { it.app.packageName })
            assertTrue(viewModel.uiState.value.results.all { it.app.kind == AppKind.GAME })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun initialSearchDropsForeignAndUnknownItems() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source = StubSearchSources(
                pages = listOf(
                    SearchPage(
                        items = listOf(game("g1"), app("a1"), app("u1", AppKind.UNKNOWN)),
                        hasMore = false,
                    ),
                ),
            )
            val viewModel = viewModel(source)
            advanceUntilIdle()

            viewModel.searchWith("游戏", setOf(AppSource.HONOR), AppCategory.GAMES)
            advanceUntilIdle()
            assertEquals(listOf("g1"), viewModel.uiState.value.results.map { it.app.packageName })

            viewModel.searchWith("应用", setOf(AppSource.HONOR), AppCategory.APPS)
            advanceUntilIdle()
            assertEquals(listOf("a1"), viewModel.uiState.value.results.map { it.app.packageName })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(sources: MarketSourceRepository) = SearchViewModel(
        sources = sources,
        historyStore = CategoryTestHistory,
        updatePrefs = CategoryTestPreferences,
        packages = CategoryTestPackages,
        downloads = CategoryTestDownloads(),
        uiPlatform = CategoryTestUiPlatform,
    )

    private fun game(packageName: String) = app(packageName, AppKind.GAME)

    private fun app(packageName: String, kind: AppKind = AppKind.APP) = MarketAppInfo(
        appId = packageName.hashCode().toLong(),
        packageName = packageName,
        displayName = packageName,
        publisherName = "",
        versionName = "1.0",
        versionCode = 1L,
        icon = "",
        apkSize = 0L,
        ratingScore = 0.0,
        source = AppSource.HONOR,
        kind = kind,
    )
}

private class StubSearchSources(private val pages: List<SearchPage>) : MarketSourceRepository {
    override suspend fun search(source: AppSource, keyword: String, page: Int): SearchPage =
        pages.getOrElse(page) { SearchPage(emptyList(), hasMore = false) }

    override suspend fun appDetail(
        source: AppSource,
        appId: Long,
        packageName: String,
        externalQuery: String?,
    ): AppDetail = error("Not used")

    override suspend fun appComments(source: AppSource, app: MarketAppInfo): AppComments = error("Not used")
    override suspend fun sameDeveloperApps(source: AppSource, app: MarketAppInfo): List<MarketAppInfo> = error("Not used")
    override suspend fun downloadMeta(source: AppSource, app: MarketAppInfo, keyword: String): DownloadMeta = error("Not used")
    override suspend fun downloadUpdateMeta(source: AppSource, app: MarketAppInfo): DownloadMeta = error("Not used")
    override suspend fun loadReconciledCachedUpdates(): List<MarketAppInfo> = emptyList()
    override fun checkUpdatesFlow(source: AppSource): Flow<List<MarketAppInfo>> = emptyFlow()
    override suspend fun checkManualUpdate(source: AppSource, request: ManualUpdateRequest): ManualUpdateResult =
        error("Not used")

    override suspend fun goldMiFeed(source: AppSource, page: Int, pageSize: Int): TodayFeedPage = error("Not used")
    override suspend fun todayArticle(source: AppSource, rId: String): TodayArticle = error("Not used")
}

private class CategoryTestDownloads : DownloadRepository {
    override val states = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    override val taskStates = MutableStateFlow<Map<DownloadTaskKey, DownloadState>>(emptyMap())
    override val installedPackages = MutableSharedFlow<String>()
    override val deltaFallbacks = MutableSharedFlow<DeltaFallback>()
    override val pendingUserAction = MutableStateFlow<InstallUserAction?>(null)
    override fun start(meta: DownloadMeta, installAfterDownload: Boolean) = Unit
    override fun install(packageName: String) = Unit
    override fun cancel(packageName: String) = Unit
    override fun cancel(packageName: String, versionCode: Long) = Unit
    override fun clear(packageName: String) = Unit
    override fun consumePendingUserAction() = Unit
}

private object CategoryTestPackages : PackageRepository {
    override val selfPackageName = "com.app.market.test"
    override val changes: SharedFlow<PackageChange> = MutableSharedFlow()
    override suspend fun installedVersionCodes(packageNames: Collection<String>): Map<String, Long> = emptyMap()
    override suspend fun installedVersionName(packageName: String): String? = null
    override suspend fun freshInstalledVersionCode(packageName: String): Long? = null
    override fun openApp(packageName: String) = false
    override fun openLink(link: String) = false
}

private object CategoryTestHistory : SearchHistoryRepository {
    override suspend fun load(): List<String> = emptyList()
    override suspend fun add(keyword: String) = Unit
    override suspend fun remove(keyword: String) = Unit
    override suspend fun clear() = Unit
}

private object CategoryTestUiPlatform : UiPlatform {
    override val packageInstallationSupported = false
    override fun showToast(message: String) = Unit
    override fun openAppSettings() = false
    override fun openUnknownSourcesSettings() = false
    override fun requestInstalledAppsPermission(onResult: (Boolean) -> Unit) = false
    override fun requestPostNotificationsPermission(onResult: () -> Unit) = false
    override suspend fun saveImageToPictures(url: String, fileName: String) = ImageSaveResult.Unsupported
}

private object CategoryTestPreferences : UpdatePreferencesRepository {
    override val initialized = MutableStateFlow(true)
    override val showSystemUpdates = MutableStateFlow(true)
    override val removeSearchAds = MutableStateFlow(false)
    override val filterQuickGames = MutableStateFlow(false)
    override val filterReservationApps = MutableStateFlow(false)
    override val showAppComments = MutableStateFlow(false)
    override val showSameDeveloper = MutableStateFlow(false)
    override val showPromotions = MutableStateFlow(false)
    override val stripAppNameSubtitle = MutableStateFlow(false)
    override val homePage = MutableStateFlow(HomePage.SEARCH)
    override val searchSources = MutableStateFlow(setOf(AppSource.HONOR))
    override val gameSources = MutableStateFlow(setOf(AppSource.HONOR))
    override val appSources = MutableStateFlow(setOf(AppSource.HONOR))
    override val todaySource = MutableStateFlow(AppSource.HONOR)
    override val updateSource = MutableStateFlow(AppSource.HONOR)
    override val permanentIgnores: StateFlow<List<IgnoredUpdate>> = MutableStateFlow(emptyList())
    override val onceIgnores: StateFlow<List<IgnoredUpdate>> = MutableStateFlow(emptyList())
    override suspend fun setShowSystemUpdates(value: Boolean) = Unit
    override suspend fun setRemoveSearchAds(value: Boolean) = Unit
    override suspend fun setFilterQuickGames(value: Boolean) = Unit
    override suspend fun setFilterReservationApps(value: Boolean) = Unit
    override suspend fun setShowAppComments(value: Boolean) = Unit
    override suspend fun setShowSameDeveloper(value: Boolean) = Unit
    override suspend fun setShowPromotions(value: Boolean) = Unit
    override suspend fun setStripAppNameSubtitle(value: Boolean) = Unit
    override suspend fun setHomePage(value: HomePage) = Unit
    override suspend fun setSearchSources(value: Set<AppSource>) = Unit
    override suspend fun setGameSources(value: Set<AppSource>) = Unit
    override suspend fun setAppSources(value: Set<AppSource>) = Unit
    override suspend fun setTodaySource(value: AppSource) = Unit
    override suspend fun setUpdateSource(value: AppSource) = Unit
    override fun isIgnored(app: MarketAppInfo) = false
    override suspend fun ignoreOnce(app: MarketAppInfo) = Unit
    override suspend fun ignorePermanently(app: MarketAppInfo) = Unit
    override suspend fun removePermanent(packageName: String) = Unit
    override suspend fun removeOnce(packageName: String) = Unit
    override suspend fun currentRemoveSearchAds() = false
    override suspend fun currentFilterQuickGames() = false
    override suspend fun currentFilterReservationApps() = false
    override suspend fun loadCachedUpdates(): List<MarketAppInfo> = emptyList()
    override suspend fun saveCachedUpdates(updates: List<MarketAppInfo>) = Unit
}
