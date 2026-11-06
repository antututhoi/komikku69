package eu.kanade.presentation.library.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
    // KMK -->
    onOpenCategoryPanel: () -> Unit = {},
    // KMK <--
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
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
                    // KMK --> 69 mod: tap the open category again for the category panel
                    onClick = {
                        if (currentPageIndex == index) onOpenCategoryPanel() else onTabItemClick(index)
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
/**
 * 69 mod: half-screen category panel at the bottom, like the Filter sheet. The library above it
 * stays usable (scroll, open entries). Closes with Back or by sliding it down by its handle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Mod69CategoryPanel(
    categories: List<Category>,
    currentIndex: Int,
    getItemCountForCategory: (Category) -> Int?,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    val density = LocalDensity.current
    var dragY by remember { mutableFloatStateOf(0f) }
    val closeAt = with(density) { 96.dp.toPx() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .graphicsLayer { translationY = dragY.coerceAtLeast(0f) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragY > closeAt) onDismiss()
                                dragY = 0f
                            },
                            onDragCancel = { dragY = 0f },
                        ) { _, amount -> dragY = (dragY + amount).coerceAtLeast(0f) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                BottomSheetDefaults.DragHandle()
            }
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0))
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(categories) { index, category ->
                    val selected = index == currentIndex
                    Surface(
                        onClick = { onPick(index) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = category.visualName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f),
                            )
                            getItemCountForCategory(category)?.let {
                                Badge(
                                    containerColor = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    contentColor = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                ) { Text(it.toString(), modifier = Modifier.padding(horizontal = 4.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
// KMK <--
