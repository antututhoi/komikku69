package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.category.genre.SortTagScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_EXISTING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_NEW
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsLibraryScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsLibraryScreen

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        return listOf(
            getCategoriesGroup(LocalNavigator.currentOrThrow, allCategories, libraryPreferences),
            getGlobalUpdateGroup(allCategories, libraryPreferences),
            getBehaviorGroup(libraryPreferences),
            // SY -->
            getSortingCategory(LocalNavigator.currentOrThrow, libraryPreferences),
            // SY <--
            // KMK -->
            getMod69Group(libraryPreferences),
            // KMK <--
        )
    }

    // KMK --> 69 mod: library and entry page extras
    @Composable
    private fun getMod69Group(libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(KMR.strings.mod69_library_extras),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.mod69UpdateDelayMs(),
                    entries = persistentMapOf(
                        0 to stringResource(KMR.strings.mod69_no_pause),
                        500 to "0.5 s",
                        1000 to "1 s",
                        2000 to "2 s",
                        5000 to "5 s",
                    ),
                    title = stringResource(KMR.strings.mod69_update_delay),
                    subtitle = stringResource(KMR.strings.mod69_update_delay_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.mod69UpdateParallel(),
                    entries = persistentMapOf(
                        1 to "1",
                        2 to "2",
                        3 to "3",
                        5 to "5 (normal)",
                        8 to "8",
                    ),
                    title = stringResource(KMR.strings.mod69_update_parallel),
                    subtitle = stringResource(KMR.strings.mod69_update_parallel_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69SortIgnoreArticles(),
                    title = stringResource(KMR.strings.mod69_sort_ignore_articles),
                    subtitle = stringResource(KMR.strings.mod69_sort_ignore_articles_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.mod69BadgeStyle(),
                    entries = persistentMapOf(
                        "number" to stringResource(KMR.strings.mod69_badge_number),
                        "dot" to stringResource(KMR.strings.mod69_badge_dot),
                        "progress" to stringResource(KMR.strings.mod69_badge_progress),
                        "percent" to stringResource(KMR.strings.mod69_badge_percent),
                        "bar" to stringResource(KMR.strings.mod69_badge_bar),
                    ),
                    title = stringResource(KMR.strings.mod69_badge_style),
                    subtitle = stringResource(KMR.strings.mod69_badge_style_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.mod69TitleLines(),
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.label_default),
                        1 to "1",
                        2 to "2",
                        3 to "3",
                    ),
                    title = stringResource(KMR.strings.mod69_title_lines),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.mod69GridSpacing(),
                    entries = persistentMapOf(
                        0 to "0 dp",
                        4 to "4 dp (normal)",
                        8 to "8 dp",
                        12 to "12 dp",
                        16 to "16 dp",
                    ),
                    title = stringResource(KMR.strings.mod69_grid_spacing),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.mod69ChapterGridColumns(),
                    entries = persistentMapOf(
                        0 to stringResource(KMR.strings.mod69_chapter_grid_off),
                        4 to "4",
                        5 to "5",
                        6 to "6",
                        8 to "8",
                    ),
                    title = stringResource(KMR.strings.mod69_chapter_grid),
                ),
                *listOf(
                    libraryPreferences.mod69BadgeDownloads() to KMR.strings.mod69_badge_corner_downloads,
                    libraryPreferences.mod69BadgeUnread() to KMR.strings.mod69_badge_corner_unread,
                    libraryPreferences.mod69BadgeLanguage() to KMR.strings.mod69_badge_corner_language,
                    libraryPreferences.mod69BadgeSource() to KMR.strings.mod69_badge_corner_source,
                ).map { (pref, titleRes) ->
                    Preference.PreferenceItem.ListPreference(
                        preference = pref,
                        entries = persistentMapOf(
                            "start" to stringResource(KMR.strings.mod69_corner_left),
                            "end" to stringResource(KMR.strings.mod69_corner_right),
                            "hide" to stringResource(KMR.strings.mod69_corner_hide),
                        ),
                        title = stringResource(titleRes),
                    )
                }.toTypedArray(),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69OnePageLibrary(),
                    title = stringResource(KMR.strings.mod69_one_page),
                    subtitle = stringResource(KMR.strings.mod69_one_page_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69StaggeredGrid(),
                    title = stringResource(KMR.strings.mod69_staggered),
                    subtitle = stringResource(KMR.strings.mod69_staggered_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69CoverOutline(),
                    title = stringResource(KMR.strings.mod69_cover_outline),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69LowMemoryCovers(),
                    title = stringResource(KMR.strings.mod69_low_memory_covers),
                    subtitle = stringResource(KMR.strings.mod69_low_memory_covers_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69ChapterThumbs(),
                    title = stringResource(KMR.strings.mod69_chapter_thumbs),
                    subtitle = stringResource(KMR.strings.mod69_chapter_thumbs_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69DedupeChapters(),
                    title = stringResource(KMR.strings.mod69_dedupe_chapters),
                    subtitle = stringResource(KMR.strings.mod69_dedupe_chapters_summary),
                ),
                Preference.PreferenceItem.EditTextPreference(
                    preference = libraryPreferences.mod69PreferredGroups(),
                    title = stringResource(KMR.strings.mod69_preferred_groups),
                    subtitle = stringResource(KMR.strings.mod69_preferred_groups_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69ConfirmRemove(),
                    title = stringResource(KMR.strings.mod69_confirm_remove),
                    subtitle = stringResource(KMR.strings.mod69_confirm_remove_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69ShowDateAdded(),
                    title = stringResource(KMR.strings.mod69_show_date_added),
                    subtitle = stringResource(KMR.strings.mod69_show_date_added_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.mod69TidyTags(),
                    title = stringResource(KMR.strings.mod69_tidy_tags),
                    subtitle = stringResource(KMR.strings.mod69_tidy_tags_summary),
                ),
            ),
        )
    }
    // KMK <--

    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val userCategoriesCount = allCategories.filterNot(Category::isSystemCategory).size

        // For default category
        val ids = listOf(libraryPreferences.defaultCategory().defaultValue()) +
            allCategories.fastMap { it.id.toInt() }
        val labels = listOf(stringResource(MR.strings.default_category_summary)) +
            allCategories.fastMap { it.visualName }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.categories),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.action_edit_categories),
                    subtitle = pluralStringResource(
                        MR.plurals.num_categories,
                        count = userCategoriesCount,
                        userCategoriesCount,
                    ),
                    onClick = { navigator.push(CategoryScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.defaultCategory(),
                    entries = ids.zip(labels).toMap().toImmutableMap(),
                    title = stringResource(MR.strings.default_category),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.categorizedDisplaySettings(),
                    title = stringResource(MR.strings.categorized_display_settings),
                    onValueChanged = {
                        if (!it) {
                            scope.launch {
                                Injekt.get<ResetCategoryFlags>().await()
                            }
                        }
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getGlobalUpdateGroup(
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val autoUpdateIntervalPref = libraryPreferences.autoUpdateInterval()
        val autoUpdateCategoriesPref = libraryPreferences.updateCategories()
        val autoUpdateCategoriesExcludePref = libraryPreferences.updateCategoriesExclude()

        val autoUpdateInterval by autoUpdateIntervalPref.collectAsState()

        val included by autoUpdateCategoriesPref.collectAsState()
        val excluded by autoUpdateCategoriesExcludePref.collectAsState()
        var showCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.categories),
                message = stringResource(MR.strings.pref_library_update_categories_details),
                items = allCategories,
                initialChecked = included.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                initialInversed = excluded.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    autoUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    autoUpdateCategoriesExcludePref.set(newExcluded.map { it.id.toString() }.toSet())
                    showCategoriesDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_library_update),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = autoUpdateIntervalPref,
                    entries = persistentMapOf(
                        0 to stringResource(MR.strings.update_never),
                        12 to stringResource(MR.strings.update_12hour),
                        24 to stringResource(MR.strings.update_24hour),
                        48 to stringResource(MR.strings.update_48hour),
                        72 to stringResource(MR.strings.update_72hour),
                        168 to stringResource(MR.strings.update_weekly),
                    ),
                    title = stringResource(MR.strings.pref_library_update_interval),
                    onValueChanged = {
                        LibraryUpdateJob.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateDeviceRestrictions(),
                    entries = persistentMapOf(
                        DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
                        DEVICE_NETWORK_NOT_METERED to stringResource(MR.strings.network_not_metered),
                        DEVICE_CHARGING to stringResource(MR.strings.charging),
                    ),
                    title = stringResource(MR.strings.pref_library_update_restriction),
                    subtitle = stringResource(MR.strings.restrictions),
                    enabled = autoUpdateInterval > 0,
                    onValueChanged = {
                        // Post to event looper to allow the preference to be updated.
                        ContextCompat.getMainExecutor(context).execute { LibraryUpdateJob.setupTask(context) }
                        true
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allCategories,
                        included = included,
                        excluded = excluded,
                    ),
                    onClick = { showCategoriesDialog = true },
                ),
                // SY -->
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.groupLibraryUpdateType(),
                    entries = persistentMapOf(
                        GroupLibraryMode.GLOBAL to stringResource(SYMR.strings.library_group_updates_global),
                        GroupLibraryMode.ALL_BUT_UNGROUPED to
                            stringResource(SYMR.strings.library_group_updates_all_but_ungrouped),
                        GroupLibraryMode.ALL to stringResource(SYMR.strings.library_group_updates_all),
                    ),
                    title = stringResource(SYMR.strings.library_group_updates),
                ),
                // SY <--
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.autoUpdateMetadata(),
                    title = stringResource(MR.strings.pref_library_update_refresh_metadata),
                    subtitle = stringResource(MR.strings.pref_library_update_refresh_metadata_summary),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateMangaRestrictions(),
                    entries = persistentMapOf(
                        MANGA_HAS_UNREAD to stringResource(MR.strings.pref_update_only_completely_read),
                        MANGA_NON_READ to stringResource(MR.strings.pref_update_only_started),
                        MANGA_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                        MANGA_OUTSIDE_RELEASE_PERIOD to stringResource(MR.strings.pref_update_only_in_release_period),
                    ),
                    title = stringResource(MR.strings.pref_library_update_smart_update),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.newShowUpdatesCount(),
                    title = stringResource(MR.strings.pref_library_update_show_tab_badge),
                ),
                // KMK -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.showUpdatingProgressBanner(),
                    title = stringResource(KMR.strings.pref_show_updating_progress_banner),
                ),
                // KMK <--
            ),
        )
    }

    @Composable
    private fun getBehaviorGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_behavior),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeToStartAction(),
                    entries = persistentMapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                            stringResource(MR.strings.action_mark_as_read),
                        LibraryPreferences.ChapterSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                    title = stringResource(MR.strings.pref_chapter_swipe_start),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeToEndAction(),
                    entries = persistentMapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                            stringResource(MR.strings.action_mark_as_read),
                        LibraryPreferences.ChapterSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                    title = stringResource(MR.strings.pref_chapter_swipe_end),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.markDuplicateReadChapterAsRead(),
                    entries = persistentMapOf(
                        MARK_DUPLICATE_CHAPTER_READ_EXISTING to
                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_existing),
                        MARK_DUPLICATE_CHAPTER_READ_NEW to
                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_new),
                    ),
                    title = stringResource(MR.strings.pref_mark_duplicate_read_chapter_read),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.hideMissingChapters(),
                    title = stringResource(MR.strings.pref_hide_missing_chapter_indicators),
                ),
                // KMK -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.showEmptyCategoriesSearch(),
                    title = stringResource(KMR.strings.pref_show_empty_categories_search),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.fetchMetadataOnAdd(),
                    title = stringResource(KMR.strings.pref_fetch_manga_metadata_on_add),
                    subtitle = stringResource(KMR.strings.pref_fetch_manga_metadata_on_add_description),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.fetchChaptersOnAdd(),
                    title = stringResource(KMR.strings.pref_fetch_manga_chapters_on_add),
                    subtitle = stringResource(KMR.strings.pref_fetch_manga_chapters_on_add_description),
                ),
                // KMK <--
            ),
        )
    }

    // SY -->
    @Composable
    fun getSortingCategory(navigator: Navigator, libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        val tagCount by libraryPreferences.sortTagsForLibrary().collectAsState()
        return Preference.PreferenceGroup(
            stringResource(SYMR.strings.pref_sorting_settings),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(SYMR.strings.pref_tag_sorting),
                    subtitle = pluralStringResource(SYMR.plurals.pref_tag_sorting_desc, tagCount.size, tagCount.size),
                    onClick = {
                        navigator.push(SortTagScreen())
                    },
                ),
            ),
        )
    }
    // SY <--
}
