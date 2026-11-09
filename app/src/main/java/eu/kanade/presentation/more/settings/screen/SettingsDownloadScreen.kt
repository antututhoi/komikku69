package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsDownloadScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsDownloadScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_downloads

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }
        val parallelSourceLimit by downloadPreferences.parallelSourceLimit().collectAsState()
        val parallelPageLimit by downloadPreferences.parallelPageLimit().collectAsState()
        // KMK --> 69 mod
        val mod69Context = androidx.compose.ui.platform.LocalContext.current
        val mod69Scope = androidx.compose.runtime.rememberCoroutineScope()
        val mod69Queued = stringResource(KMR.strings.mod69_redownload_queued)
        fun mod69RedownloadAll(mode: String) {
            mod69Scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val n = if (mode == "convert") {
                    eu.kanade.tachiyomi.data.download.Mod69PageQuality.convertEverything()
                } else {
                    eu.kanade.tachiyomi.data.download.Mod69PageQuality.redownloadEverything(mode)
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(mod69Context, mod69Queued.format(n), android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        // KMK <--
        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.downloadOnlyOverWifi(),
                title = stringResource(MR.strings.connected_to_wifi),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.saveChaptersAsCBZ(),
                title = stringResource(MR.strings.save_chapter_as_cbz),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.splitTallImages(),
                title = stringResource(MR.strings.split_tall_images),
                subtitle = stringResource(MR.strings.split_tall_images_summary),
            ),
            // KMK -->
            Preference.PreferenceItem.ListPreference(
                preference = downloadPreferences.mod69SpeedLimitKb(),
                entries = persistentMapOf(
                    0 to stringResource(KMR.strings.mod69_no_limit),
                    256 to "256 KB/s",
                    512 to "512 KB/s",
                    1024 to "1 MB/s",
                    2048 to "2 MB/s",
                    5120 to "5 MB/s",
                ),
                title = stringResource(KMR.strings.mod69_speed_limit),
                subtitle = stringResource(KMR.strings.mod69_speed_limit_summary),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = downloadPreferences.mod69DownloadDelayMs(),
                entries = persistentMapOf(
                    0 to stringResource(KMR.strings.mod69_no_pause),
                    250 to "0.25 s",
                    500 to "0.5 s",
                    1000 to "1 s",
                    2000 to "2 s",
                ),
                title = stringResource(KMR.strings.mod69_download_delay),
                subtitle = stringResource(KMR.strings.mod69_download_delay_summary),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = downloadPreferences.mod69DownloadRetries(),
                entries = persistentMapOf(
                    0 to "0",
                    1 to "1",
                    3 to "3 (normal)",
                    5 to "5",
                    8 to "8",
                ),
                title = stringResource(KMR.strings.mod69_download_retries),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = downloadPreferences.mod69PageQuality(),
                entries = persistentMapOf(
                    "original" to stringResource(KMR.strings.mod69_page_quality_original),
                    "smaller" to stringResource(KMR.strings.mod69_page_quality_smaller),
                ),
                title = stringResource(KMR.strings.mod69_page_quality),
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = downloadPreferences.mod69PageMaxWidthText(),
                title = stringResource(KMR.strings.mod69_page_max_width),
                subtitle = "%s px (0 = no limit)",
                onValueChanged = { it.trim().toIntOrNull()?.let { n -> n >= 0 } ?: false },
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = downloadPreferences.mod69PageMaxHeightText(),
                title = stringResource(KMR.strings.mod69_page_max_height),
                subtitle = "%s px (0 = no limit)",
                onValueChanged = { it.trim().toIntOrNull()?.let { n -> n >= 0 } ?: false },
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.mod69KeepTallPages(),
                title = stringResource(KMR.strings.mod69_keep_tall_pages),
                subtitle = stringResource(KMR.strings.mod69_keep_tall_pages_summary),
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = downloadPreferences.mod69PageWebpQualityText(),
                title = stringResource(KMR.strings.mod69_page_webp_quality),
                subtitle = "%s (1 – 100)",
                onValueChanged = { it.trim().toIntOrNull()?.let { n -> n in 1..100 } ?: false },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(KMR.strings.mod69_redownload_all_smaller),
                onClick = { mod69RedownloadAll("smaller") },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(KMR.strings.mod69_redownload_all_original),
                onClick = { mod69RedownloadAll("original") },
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.mod69CategoryFolders(),
                title = stringResource(KMR.strings.mod69_category_folders),
                subtitle = stringResource(KMR.strings.mod69_category_folders_summary),
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = downloadPreferences.mod69ExcludedFolderCategories(),
                title = stringResource(KMR.strings.mod69_excluded_folder_categories),
                subtitle = stringResource(KMR.strings.mod69_excluded_folder_categories_summary),
                onValueChanged = {
                    // move the entries out of the excluded folders right away (to their next category)
                    mod69Scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        kotlinx.coroutines.delay(500)
                        runCatching { eu.kanade.tachiyomi.data.download.Mod69DownloadFolders.moveAll(mod69Context) }
                    }
                    true
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(KMR.strings.mod69_move_to_category_folders),
                subtitle = stringResource(KMR.strings.mod69_move_to_category_folders_summary),
                onClick = {
                    mod69Scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val n = runCatching { eu.kanade.tachiyomi.data.download.Mod69DownloadFolders.moveAll(mod69Context) }.getOrDefault(0)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(mod69Context, "Moved $n entries", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                },
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.mod69MigrateMoveDownloads(),
                title = stringResource(KMR.strings.mod69_migrate_move_downloads),
                subtitle = stringResource(KMR.strings.mod69_migrate_move_downloads_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.mod69ScanlatorInChapterNames(),
                title = stringResource(KMR.strings.mod69_scanlator_in_names),
                subtitle = stringResource(KMR.strings.mod69_scanlator_in_names_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = downloadPreferences.mod69ShortNames(),
                title = stringResource(KMR.strings.mod69_short_names),
                subtitle = stringResource(KMR.strings.mod69_short_names_summary),
            ),
            Preference.PreferenceItem.EditTextPreference(
                preference = downloadPreferences.mod69ShortNameLengthText(),
                title = stringResource(KMR.strings.mod69_short_name_length),
                subtitle = "%s (30 – 200)",
                onValueChanged = { it.trim().toIntOrNull()?.let { n -> n in 30..200 } ?: false },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(KMR.strings.mod69_rename_downloads),
                subtitle = stringResource(KMR.strings.mod69_rename_downloads_summary),
                onClick = {
                    mod69Scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { eu.kanade.tachiyomi.data.download.Mod69RenameDownloads.run(mod69Context) }
                    }
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(KMR.strings.mod69_convert_all),
                subtitle = stringResource(KMR.strings.mod69_convert_all_summary),
                onClick = { mod69RedownloadAll("convert") },
            ),
            // KMK <--
            Preference.PreferenceItem.SliderPreference(
                value = parallelSourceLimit,
                valueRange = 1..10,
                title = stringResource(MR.strings.pref_download_concurrent_sources),
                onValueChanged = { downloadPreferences.parallelSourceLimit().set(it) },
            ),
            Preference.PreferenceItem.SliderPreference(
                value = parallelPageLimit,
                valueRange = 1..15,
                title = stringResource(MR.strings.pref_download_concurrent_pages),
                subtitle = stringResource(MR.strings.pref_download_concurrent_pages_summary),
                onValueChanged = { downloadPreferences.parallelPageLimit().set(it) },
            ),
            getDeleteChaptersGroup(
                downloadPreferences = downloadPreferences,
                categories = allCategories,
            ),
            getAutoDownloadGroup(
                downloadPreferences = downloadPreferences,
                allCategories = allCategories,
            ),
            getDownloadAheadGroup(downloadPreferences = downloadPreferences),
            // KMK -->
            getDownloadCacheRenewInterval(downloadPreferences = downloadPreferences),
            // KMK <--
        )
    }

    @Composable
    private fun getDeleteChaptersGroup(
        downloadPreferences: DownloadPreferences,
        categories: List<Category>,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_delete_chapters),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.removeAfterMarkedAsRead(),
                    title = stringResource(MR.strings.pref_remove_after_marked_as_read),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.removeAfterReadSlots(),
                    entries = persistentMapOf(
                        -1 to stringResource(MR.strings.disabled),
                        0 to stringResource(MR.strings.last_read_chapter),
                        1 to stringResource(MR.strings.second_to_last),
                        2 to stringResource(MR.strings.third_to_last),
                        3 to stringResource(MR.strings.fourth_to_last),
                        4 to stringResource(MR.strings.fifth_to_last),
                    ),
                    title = stringResource(MR.strings.pref_remove_after_read),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.removeBookmarkedChapters(),
                    title = stringResource(MR.strings.pref_remove_bookmarked_chapters),
                ),
                getExcludedCategoriesPreference(
                    downloadPreferences = downloadPreferences,
                    categories = { categories },
                ),
            ),
        )
    }

    @Composable
    private fun getExcludedCategoriesPreference(
        downloadPreferences: DownloadPreferences,
        categories: () -> List<Category>,
    ): Preference.PreferenceItem.MultiSelectListPreference {
        return Preference.PreferenceItem.MultiSelectListPreference(
            preference = downloadPreferences.removeExcludeCategories(),
            entries = categories()
                .associate { it.id.toString() to it.visualName }
                .toImmutableMap(),
            title = stringResource(MR.strings.pref_remove_exclude_categories),
        )
    }

    @Composable
    private fun getAutoDownloadGroup(
        downloadPreferences: DownloadPreferences,
        allCategories: List<Category>,
    ): Preference.PreferenceGroup {
        val downloadNewChaptersPref = downloadPreferences.downloadNewChapters()
        val downloadNewUnreadChaptersOnlyPref = downloadPreferences.downloadNewUnreadChaptersOnly()
        val downloadNewChapterCategoriesPref = downloadPreferences.downloadNewChapterCategories()
        val downloadNewChapterCategoriesExcludePref = downloadPreferences.downloadNewChapterCategoriesExclude()

        val downloadNewChapters by downloadNewChaptersPref.collectAsState()

        val included by downloadNewChapterCategoriesPref.collectAsState()
        val excluded by downloadNewChapterCategoriesExcludePref.collectAsState()
        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.categories),
                message = stringResource(MR.strings.pref_download_new_categories_details),
                items = allCategories,
                initialChecked = included.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                initialInversed = excluded.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    downloadNewChapterCategoriesPref.set(newIncluded.fastMap { it.id.toString() }.toSet())
                    downloadNewChapterCategoriesExcludePref.set(newExcluded.fastMap { it.id.toString() }.toSet())
                    showDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_auto_download),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadNewChaptersPref,
                    title = stringResource(MR.strings.pref_download_new),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadNewUnreadChaptersOnlyPref,
                    title = stringResource(MR.strings.pref_download_new_unread_chapters_only),
                    enabled = downloadNewChapters,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allCategories,
                        included = included,
                        excluded = excluded,
                    ),
                    enabled = downloadNewChapters,
                    onClick = { showDialog = true },
                ),
            ),
        )
    }

    @Composable
    private fun getDownloadAheadGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.download_ahead),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.autoDownloadWhileReading(),
                    entries = listOf(0, 2, 3, 5, 10)
                        .associateWith {
                            if (it == 0) {
                                stringResource(MR.strings.disabled)
                            } else {
                                pluralStringResource(MR.plurals.next_unread_chapters, count = it, it)
                            }
                        }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.auto_download_while_reading),
                ),
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.download_ahead_info)),
            ),
        )
    }

    // KMK -->
    @Composable
    private fun getDownloadCacheRenewInterval(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(KMR.strings.download_cache_renew_interval),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = downloadPreferences.downloadCacheRenewInterval(),
                    entries = persistentMapOf(
                        -1 to stringResource(KMR.strings.download_cache_renew_interval_manual),
                        1 to stringResource(KMR.strings.download_cache_renew_interval_1hour),
                        2 to stringResource(KMR.strings.download_cache_renew_interval_2hour),
                        6 to stringResource(KMR.strings.download_cache_renew_interval_6hour),
                        12 to stringResource(KMR.strings.download_cache_renew_interval_12hour),
                        24 to stringResource(KMR.strings.download_cache_renew_interval_24hour),
                    ),
                    title = stringResource(KMR.strings.download_cache_renew_interval),
                ),
                Preference.PreferenceItem.InfoPreference(stringResource(KMR.strings.download_cache_renew_interval_info)),
            ),
        )
    }
    // KMK <--
}
