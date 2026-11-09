package eu.kanade.presentation.library.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    onClickSyncNow: () -> Unit,
    // SY -->
    onClickSyncExh: (() -> Unit)?,
    isSyncEnabled: Boolean,
    // SY <--
    // KMK -->
    onClickUpdateMarked: () -> Unit,
    onClickFindDuplicates: () -> Unit,
    onClickMarkSelection: (Boolean) -> Unit,
    onClickTrackSelection: () -> Unit,
    // KMK <--
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    onInvalidateDownloadCache: (Context) -> Unit,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
        // KMK -->
        onClickMarkSelection = onClickMarkSelection,
        onClickTrackSelection = onClickTrackSelection,
        // KMK <--
    )
    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomManga = onClickOpenRandomManga,
        onClickSyncNow = onClickSyncNow,
        // KMK -->
        onClickUpdateMarked = onClickUpdateMarked,
        onClickFindDuplicates = onClickFindDuplicates,
        // KMK <--
        // SY -->
        onClickSyncExh = onClickSyncExh,
        isSyncEnabled = isSyncEnabled,
        // SY <--
        scrollBehavior = scrollBehavior,
        onInvalidateDownloadCache = onInvalidateDownloadCache,
    )
}

@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    onClickSyncNow: () -> Unit,
    // KMK -->
    onClickUpdateMarked: () -> Unit,
    onClickFindDuplicates: () -> Unit,
    // KMK <--
    // SY -->
    onClickSyncExh: (() -> Unit)?,
    isSyncEnabled: Boolean,
    // SY <--
    scrollBehavior: TopAppBarScrollBehavior?,
    onInvalidateDownloadCache: (Context) -> Unit,
) {
    val context = LocalContext.current
    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
    // KMK --> 69 mod L3: search tips dialog
    var mod69ShowTips by remember { mutableStateOf(false) }
    val mod69Navigator = cafe.adriel.voyager.navigator.LocalNavigator.current
    if (mod69ShowTips) {
        AlertDialog(
            onDismissRequest = { mod69ShowTips = false },
            title = { Text(stringResource(KMR.strings.mod69_search_tips)) },
            text = { Text(stringResource(KMR.strings.mod69_search_tips_text)) },
            confirmButton = {
                TextButton(onClick = { mod69ShowTips = false }) { Text(stringResource(MR.strings.action_ok)) }
            },
        )
    }
    // KMK <--
    SearchToolbar(
        titleContent = {
            // KMK --> 69 mod: tap "Library" (with the count) for the category panel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { Mod69LibraryPanel.open = !Mod69LibraryPanel.open },
            ) {
                // KMK <--
                Text(
                    text = title.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, false),
                    overflow = TextOverflow.Ellipsis,
                )
                if (title.numberOfManga != null) {
                    Pill(
                        text = "${title.numberOfManga}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillAlpha),
                        fontSize = 14.sp,
                    )
                }
            }
        },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        actions = {
            val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = Icons.Outlined.FilterList,
                        iconTint = filterTint,
                        onClick = onClickFilter,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_library),
                        onClick = onClickGlobalUpdate,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_category),
                        onClick = onClickRefresh,
                    ),
                    // KMK -->
                    AppBar.OverflowAction(
                        title = stringResource(KMR.strings.mod69_update_marked),
                        onClick = onClickUpdateMarked,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(KMR.strings.mod69_search_tips),
                        onClick = { mod69ShowTips = true },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(KMR.strings.mod69_find_duplicates),
                        onClick = onClickFindDuplicates,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(KMR.strings.mod69_anilist_list),
                        onClick = { mod69Navigator?.push(eu.kanade.tachiyomi.ui.mod69.Mod69AniListScreen()) },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(KMR.strings.mod69_suggestions),
                        onClick = { mod69Navigator?.push(eu.kanade.tachiyomi.ui.mod69.Mod69SuggestionsScreen()) },
                    ),
                    // KMK <--
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_open_random_manga),
                        onClick = onClickOpenRandomManga,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.pref_invalidate_download_cache),
                        onClick = {
                            onInvalidateDownloadCache(context)
                        },
                    ),
                ).builder().apply {
                    // SY -->
                    if (onClickSyncExh != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(SYMR.strings.sync_favorites),
                                onClick = onClickSyncExh,
                            ),
                        )
                    }
                    if (isSyncEnabled) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(SYMR.strings.sync_library),
                                onClick = onClickSyncNow,
                            ),
                        )
                    }
                    // SY <--
                }.build(),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    // KMK -->
    onClickMarkSelection: (Boolean) -> Unit,
    onClickTrackSelection: () -> Unit,
    // KMK <--
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                    // KMK -->
                    AppBar.OverflowAction(
                        title = stringResource(KMR.strings.mod69_mark_for_update),
                        onClick = { onClickMarkSelection(true) },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(KMR.strings.mod69_unmark_for_update),
                        onClick = { onClickMarkSelection(false) },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(KMR.strings.mod69_bulk_track),
                        onClick = onClickTrackSelection,
                    ),
                    // KMK <--
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfManga: Int? = null,
)
