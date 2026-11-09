package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.ActionButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderPageActionsDialog(
    onDismissRequest: () -> Unit,
    onSetAsCover: (/* SY --> */useExtraPage: Boolean/* SY <-- */) -> Unit,
    onShare: (copyToClipboard: Boolean, /* SY --> */useExtraPage: Boolean/* SY <-- */) -> Unit,
    onSave: (/* SY --> */useExtraPage: Boolean/* SY <-- */) -> Unit,
    // SY -->
    onShareCombined: (copyToClipboard: Boolean) -> Unit,
    onSaveCombined: () -> Unit,
    hasExtraPage: Boolean,
    // SY <--
    // KMK -->
    mod69MangaId: Long = -1L,
    mod69ChapterId: Long = -1L,
    mod69ChapterName: String = "",
    mod69PageIndex: Int = 0,
    onMod69Jump: (chapterId: Long, pageIndex: Int) -> Unit = { _, _ -> },
    // KMK <--
) {
    // KMK -->
    var showMod69List by remember { mutableStateOf(false) }
    var mod69Marked by remember {
        mutableStateOf(Mod69PageBookmarks.isBookmarked(mod69MangaId, mod69ChapterId, mod69PageIndex))
    }
    // KMK <--
    var showSetCoverDialog by remember { mutableStateOf(false) }
    // SY -->
    var useExtraPage by remember { mutableStateOf(false) }
    // SY <--

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        // SY -->
                        if (hasExtraPage) {
                            SYMR.strings.action_set_first_page_cover
                        } else {
                            MR.strings.set_as_cover
                        },
                        // SY <--
                    ),
                    icon = Icons.Outlined.Photo,
                    onClick = { showSetCoverDialog = true },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        // SY -->
                        if (hasExtraPage) {
                            KMR.strings.action_copy_to_clipboard_first_page
                        } else {
                            MR.strings.action_copy_to_clipboard
                        },
                        // SY <--
                    ),
                    icon = Icons.Outlined.ContentCopy,
                    onClick = {
                        // SY -->
                        onShare(true, false)
                        // SY <--
                        onDismissRequest()
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        // SY -->
                        if (hasExtraPage) {
                            SYMR.strings.action_share_first_page
                        } else {
                            MR.strings.action_share
                        },
                        // SY <--
                    ),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        // SY -->
                        onShare(false, false)
                        // SY <--
                        onDismissRequest()
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        // SY -->
                        if (hasExtraPage) {
                            SYMR.strings.action_save_first_page
                        } else {
                            MR.strings.action_save
                        },
                        // SY <--
                    ),
                    icon = Icons.Outlined.Save,
                    onClick = {
                        // SY -->
                        onSave(false)
                        // SY <--
                        onDismissRequest()
                    },
                )
            }
            // KMK --> 69 mod R17: bookmark this page, see the page bookmarks of this entry
            if (mod69MangaId > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(
                            if (mod69Marked) KMR.strings.mod69_unbookmark_page else KMR.strings.mod69_bookmark_page,
                        ),
                        icon = Icons.Outlined.Bookmark,
                        onClick = {
                            mod69Marked = Mod69PageBookmarks.toggle(
                                Mod69PageBookmarks.Item(mod69MangaId, mod69ChapterId, mod69PageIndex, mod69ChapterName),
                            )
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(KMR.strings.mod69_page_bookmarks),
                        icon = Icons.Outlined.Bookmarks,
                        onClick = { showMod69List = true },
                    )
                }
            }
            // KMK <--
            if (hasExtraPage) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_set_second_page_cover),
                        icon = Icons.Outlined.Photo,
                        onClick = {
                            showSetCoverDialog = true
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(KMR.strings.action_copy_to_clipboard_second_page),
                        icon = Icons.Outlined.ContentCopy,
                        onClick = {
                            onShare(true, true)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_share_second_page),
                        icon = Icons.Outlined.Share,
                        onClick = {
                            onShare(false, true)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_save_second_page),
                        icon = Icons.Outlined.Save,
                        onClick = {
                            onSave(true)
                            onDismissRequest()
                        },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(KMR.strings.action_copy_to_clipboard_combined_page),
                        icon = Icons.Outlined.ContentCopy,
                        onClick = {
                            onShareCombined(true)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_share_combined_page),
                        icon = Icons.Outlined.Share,
                        onClick = {
                            onShareCombined(false)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(SYMR.strings.action_save_combined_page),
                        icon = Icons.Outlined.Save,
                        onClick = {
                            onSaveCombined()
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }

    // KMK -->
    if (showMod69List) {
        val items = remember { Mod69PageBookmarks.forManga(mod69MangaId) }
        AlertDialog(
            onDismissRequest = { showMod69List = false },
            title = { Text(stringResource(KMR.strings.mod69_page_bookmarks)) },
            text = {
                if (items.isEmpty()) {
                    Text(stringResource(KMR.strings.mod69_no_page_bookmarks))
                } else {
                    Column {
                        items.forEach { item ->
                            TextButton(
                                onClick = {
                                    showMod69List = false
                                    onMod69Jump(item.chapterId, item.pageIndex)
                                    onDismissRequest()
                                },
                            ) {
                                Text("${item.chapterName} - ${item.pageIndex + 1}")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMod69List = false }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
    // KMK <--

    if (showSetCoverDialog) {
        SetCoverDialog(
            onConfirm = {
                // SY -->
                onSetAsCover(useExtraPage)
                showSetCoverDialog = false
                useExtraPage = false
                // SY <--
            },
            onDismiss = { showSetCoverDialog = false },
        )
    }
}

@Composable
private fun SetCoverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        text = {
            Text(stringResource(MR.strings.confirm_set_image_as_cover))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismiss,
    )
}
