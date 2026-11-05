package eu.kanade.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.category.visualName
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.material.TabText

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
    // KMK --> 69 mod: tap the open category again to get a full-height list of all categories
    var mod69ShowSheet by remember { mutableStateOf(false) }
    if (mod69ShowSheet) {
        Mod69CategorySheet(
            categories = categories,
            currentIndex = currentPageIndex,
            getItemCountForCategory = getItemCountForCategory,
            onPick = {
                mod69ShowSheet = false
                onTabItemClick(it)
            },
            onDismiss = { mod69ShowSheet = false },
        )
    }
    // KMK <--
    Column(modifier = Modifier.zIndex(2f)) {
        PrimaryScrollableTabRow(
            selectedTabIndex = currentPageIndex,
            edgePadding = 0.dp,
            // TODO: use default when width is fixed upstream
            // https://issuetracker.google.com/issues/242879624
            divider = {},
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = currentPageIndex == index,
                    // KMK -->
                    onClick = {
                        if (currentPageIndex == index) mod69ShowSheet = true else onTabItemClick(index)
                    },
                    // KMK <--
                    text = {
                        TabText(
                            text = category.visualName,
                            badgeCount = getItemCountForCategory(category),
                        )
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        HorizontalDivider()
    }
}

// KMK -->
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Mod69CategorySheet(
    categories: List<Category>,
    currentIndex: Int,
    getItemCountForCategory: (Category) -> Int?,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 3).coerceAtLeast(0))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight(),
        ) {
            itemsIndexed(categories) { index, category ->
                val selected = index == currentIndex
                ListItem(
                    headlineContent = {
                        Text(
                            text = category.visualName,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    trailingContent = {
                        getItemCountForCategory(category)?.let { Text(it.toString()) }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                    ),
                    modifier = Modifier.clickable { onPick(index) },
                )
            }
        }
    }
}
// KMK <--
