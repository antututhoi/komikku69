package eu.kanade.presentation.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.ui.history.HistorySettingsScreenModel
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.history.service.HistoryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun HistoryFilterDialog(
    onDismissRequest: () -> Unit,
    screenModel: HistorySettingsScreenModel,
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = persistentListOf(
            stringResource(MR.strings.action_filter),
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            FilterSheet(screenModel = screenModel)
        }
    }
}

@Composable
private fun ColumnScope.FilterSheet(
    screenModel: HistorySettingsScreenModel,
) {
    val filterUnfinishedManga by screenModel.historyPreferences.filterUnfinishedManga().collectAsState()
    TriStateItem(
        label = stringResource(KMR.strings.action_filter_unfinished_manga),
        state = filterUnfinishedManga,
        onClick = { screenModel.toggleFilter(HistoryPreferences::filterUnfinishedManga) },
    )

    val filterUnfinishedChapter by screenModel.historyPreferences.filterUnfinishedChapter().collectAsState()
    TriStateItem(
        label = stringResource(KMR.strings.action_filter_unfinished_chapter),
        state = filterUnfinishedChapter,
        onClick = { screenModel.toggleFilter(HistoryPreferences::filterUnfinishedChapter) },
    )

    val filterNonLibraryManga by screenModel.historyPreferences.filterNonLibraryManga().collectAsState()
    TriStateItem(
        label = stringResource(KMR.strings.action_filter_non_library_entries),
        state = filterNonLibraryManga,
        onClick = { screenModel.toggleFilter(HistoryPreferences::filterNonLibraryManga) },
    )

    HorizontalDivider(modifier = Modifier.padding(MaterialTheme.padding.small))

    val panoramaCover by screenModel.historyPreferences.usePanoramaCover().collectAsState()

    Row(
        modifier = Modifier
            .clickable { screenModel.toggleSwitch(HistoryPreferences::usePanoramaCover) }
            .fillMaxWidth()
            .padding(horizontal = SettingsItemsPaddings.Horizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(KMR.strings.action_panorama_cover),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )

        Switch(
            checked = panoramaCover,
            onCheckedChange = { screenModel.toggleSwitch(HistoryPreferences::usePanoramaCover) },
        )
    }

    // KMK --> 69 mod: cover size
    val mod69Scale by screenModel.historyPreferences.mod69CoverScale().collectAsState()
    Text(
        text = stringResource(KMR.strings.mod69_cover_size),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = SettingsItemsPaddings.Horizontal, vertical = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsItemsPaddings.Horizontal),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(75, 100, 125, 150, 200).forEach { v ->
            FilterChip(
                selected = mod69Scale == v,
                onClick = { screenModel.historyPreferences.mod69CoverScale().set(v) },
                label = { Text("$v%") },
            )
        }
    }
    // KMK <--
}
