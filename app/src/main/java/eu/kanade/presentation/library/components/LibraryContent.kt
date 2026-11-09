package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

@Composable
fun LibraryContent(
    categories: List<Category>,
    // KMK -->
    activeCategoryIndex: Int,
    // KMK <--
    searchQuery: String?,
    selection: Set<Long>,
    contentPadding: PaddingValues,
    currentPage: Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onClickManga: (Long) -> Unit,
    onContinueReadingClicked: ((LibraryManga) -> Unit)?,
    onToggleSelection: (Category, LibraryManga) -> Unit,
    onToggleRangeSelection: (Category, LibraryManga) -> Unit,
    onRefresh: () -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    getItemCountForCategory: (Category) -> Int?,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForCategory: (Category) -> List<LibraryItem>,
) {
    // KMK --> 69 mod: half-screen category panel over the library
    var mod69PanelOpen by Mod69LibraryPanel::open
    val pagerState = rememberPagerState(currentPage) { categories.size }
    // 69 mod L1: all categories on one page (not while searching)
    val mod69OnePagePref by androidx.compose.runtime.remember {
        uy.kohesive.injekt.Injekt.get<tachiyomi.domain.library.service.LibraryPreferences>().mod69OnePageLibrary()
    }.collectAsState()
    val mod69OnePage = mod69OnePagePref && searchQuery.isNullOrEmpty() && categories.size > 1
    val mod69GridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    Box(modifier = Modifier.fillMaxSize()) {
        // KMK <--
        Column(
            modifier = Modifier.padding(
                top = contentPadding.calculateTopPadding(),
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
            ),
        ) {
            val scope = rememberCoroutineScope()
            var isRefreshing by remember(pagerState.currentPage) { mutableStateOf(false) }

            if (!mod69OnePage && showPageTabs && categories.isNotEmpty() && (categories.size > 1 || !categories.first().isSystemCategory)) {
                LaunchedEffect(categories) {
                    // KMK -->
                    val targetPage = when {
                        categories.isEmpty() -> 0
                        activeCategoryIndex != pagerState.currentPage -> activeCategoryIndex.coerceAtMost(categories.size - 1)
                        pagerState.currentPage >= categories.size -> categories.size - 1
                        else -> pagerState.currentPage
                    }
                    if (targetPage != pagerState.currentPage) {
                        pagerState.scrollToPage(targetPage)
                    }
                    // KMK <--
                }
                LibraryTabs(
                    categories = categories,
                    pagerState = pagerState,
                    getItemCountForCategory = getItemCountForCategory,
                    onTabItemClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(it)
                        }
                    },
                    // KMK -->
                    onOpenCategoryPanel = { mod69PanelOpen = !mod69PanelOpen },
                    // KMK <--
                )
            }

            PullRefresh(
                refreshing = isRefreshing,
                enabled = selection.isEmpty(),
                onRefresh = {
                    val started = onRefresh()
                    if (!started) return@PullRefresh
                    scope.launch {
                        // Fake refresh status but hide it after a second as it's a long running task
                        isRefreshing = true
                        delay(1.seconds)
                        isRefreshing = false
                    }
                },
            ) {
                // KMK -->
                if (mod69OnePage) {
                    Mod69OnePageLibrary(
                        categories = categories,
                        gridState = mod69GridState,
                        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                        selection = selection,
                        getDisplayMode = getDisplayMode,
                        getColumnsForOrientation = getColumnsForOrientation,
                        getItemsForCategory = getItemsForCategory,
                        onClickManga = { category, manga ->
                            if (selection.isNotEmpty()) {
                                onToggleSelection(category, manga)
                            } else {
                                onClickManga(manga.manga.id)
                            }
                        },
                        onLongClickManga = onToggleRangeSelection,
                    )
                    return@PullRefresh
                }
                // KMK <--
                LibraryPager(
                    state = pagerState,
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    hasActiveFilters = hasActiveFilters,
                    selection = selection,
                    searchQuery = searchQuery,
                    onGlobalSearchClicked = onGlobalSearchClicked,
                    getCategoryForPage = { page -> categories[page] },
                    getDisplayMode = getDisplayMode,
                    getColumnsForOrientation = getColumnsForOrientation,
                    getItemsForCategory = getItemsForCategory,
                    onClickManga = { category, manga ->
                        if (selection.isNotEmpty()) {
                            onToggleSelection(category, manga)
                        } else {
                            onClickManga(manga.manga.id)
                        }
                    },
                    onLongClickManga = onToggleRangeSelection,
                    onClickContinueReading = onContinueReadingClicked,
                )
            }

            LaunchedEffect(pagerState.currentPage) {
                onChangeCurrentPage(pagerState.currentPage)
            }
        }
        // KMK -->
        if (mod69PanelOpen && categories.size > 1) {
            val mod69Scope = rememberCoroutineScope()
            Mod69CategoryPanel(
                categories = categories,
                currentIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex),
                getItemCountForCategory = getItemCountForCategory,
                onPick = { index ->
                    mod69Scope.launch {
                        if (mod69OnePage) {
                            categories.getOrNull(index)?.let { category ->
                                Mod69OnePage.headerIndex[category.id]?.let { mod69GridState.scrollToItem(it) }
                            }
                        } else {
                            pagerState.scrollToPage(index)
                        }
                    }
                },
                onDismiss = { mod69PanelOpen = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = contentPadding.calculateBottomPadding()),
            )
        }
    }
    // KMK <--
}
