package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.Mod69ReorderPreferenceWidget
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.Companion.zoomWideImagesAllowedList
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.WebtoonScaleType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat

object SettingsReaderScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsReaderScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_reader

    @Composable
    override fun getPreferences(): List<Preference> {
        val readerPref = remember { Injekt.get<ReaderPreferences>() }
        // SY -->
        val forceHorizontalSeekbar by readerPref.forceHorizontalSeekbar().collectAsState()
        // SY <--

        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = readerPref.defaultReadingMode(),
                entries = ReadingMode.entries.drop(1)
                    .associate { it.flagValue to stringResource(it.stringRes) }
                    .toImmutableMap(),
                title = stringResource(MR.strings.pref_viewer_type),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPref.doubleTapAnimSpeed(),
                entries = persistentMapOf(
                    1 to stringResource(MR.strings.double_tap_anim_speed_0),
                    500 to stringResource(MR.strings.double_tap_anim_speed_normal),
                    250 to stringResource(MR.strings.double_tap_anim_speed_fast),
                ),
                title = stringResource(MR.strings.pref_double_tap_anim_speed),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.showReadingMode(),
                title = stringResource(MR.strings.pref_show_reading_mode),
                subtitle = stringResource(MR.strings.pref_show_reading_mode_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.showNavigationOverlayOnStart(),
                title = stringResource(MR.strings.pref_show_navigation_mode),
                subtitle = stringResource(MR.strings.pref_show_navigation_mode_summary),
            ),
            // KMK -->
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.smallerTapZone(),
                title = stringResource(KMR.strings.pref_viewer_nav_smaller_tap_zone),
            ),
            // KMK <--
            // SY -->
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.forceHorizontalSeekbar(),
                title = stringResource(SYMR.strings.pref_force_horz_seekbar),
                subtitle = stringResource(SYMR.strings.pref_force_horz_seekbar_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.landscapeVerticalSeekbar(),
                title = stringResource(SYMR.strings.pref_show_vert_seekbar_landscape),
                subtitle = stringResource(SYMR.strings.pref_show_vert_seekbar_landscape_summary),
                enabled = !forceHorizontalSeekbar,
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.leftVerticalSeekbar(),
                title = stringResource(SYMR.strings.pref_left_handed_vertical_seekbar),
                subtitle = stringResource(SYMR.strings.pref_left_handed_vertical_seekbar_summary),
                enabled = !forceHorizontalSeekbar,
            ),
            // SY <--
            /* SY -->
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.pageTransitions(),
                title = stringResource(MR.strings.pref_page_transitions),
            ),
            SY <-- */
            getDisplayGroup(readerPreferences = readerPref),
            getEInkGroup(readerPreferences = readerPref),
            getReadingGroup(readerPreferences = readerPref),
            getPagedGroup(readerPreferences = readerPref),
            getWebtoonGroup(readerPreferences = readerPref),
            // SY -->
            getContinuousVerticalGroup(readerPreferences = readerPref),
            // SY <--
            getNavigationGroup(readerPreferences = readerPref),
            getActionsGroup(readerPreferences = readerPref),
            // SY -->
            getPageDownloadingGroup(readerPreferences = readerPref),
            getForkSettingsGroup(readerPreferences = readerPref),
            // SY <--
            // KMK -->
            getMod69Group(readerPreferences = readerPref),
            // KMK <--
        )
    }

    // KMK --> 69 mod: small extras for the reader
    @Composable
    private fun getMod69Group(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val infoBar by readerPreferences.mod69InfoBar().collectAsState()
        val seekbarHeightPref = readerPreferences.mod69SeekbarHeight()
        val seekbarHeight by seekbarHeightPref.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(KMR.strings.mod69_reader_extras),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69CutoutAvoidSideways(),
                    title = stringResource(KMR.strings.mod69_cutout_sideways),
                    subtitle = stringResource(KMR.strings.mod69_cutout_sideways_summary),
                    enabled = LocalView.current.hasDisplayCutout(),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = seekbarHeight,
                    valueRange = 30..100,
                    title = stringResource(KMR.strings.mod69_seekbar_height),
                    valueString = "$seekbarHeight%",
                    onValueChanged = { seekbarHeightPref.set(it) },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69PreloadWifiOnly(),
                    title = stringResource(KMR.strings.mod69_preload_wifi),
                    subtitle = stringResource(KMR.strings.mod69_preload_wifi_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69InfoBar(),
                    title = stringResource(KMR.strings.mod69_info_bar),
                    subtitle = stringResource(KMR.strings.mod69_info_bar_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69InfoBarClock(),
                    title = stringResource(KMR.strings.mod69_info_bar_clock),
                    enabled = infoBar,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69InfoBarBattery(),
                    title = stringResource(KMR.strings.mod69_info_bar_battery),
                    enabled = infoBar,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69InfoBarChapter(),
                    title = stringResource(KMR.strings.mod69_info_bar_chapter),
                    enabled = infoBar,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69InfoBarPage(),
                    title = stringResource(KMR.strings.mod69_info_bar_page),
                    enabled = infoBar,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.mod69InfoBarPosition(),
                    entries = persistentMapOf(
                        "top" to stringResource(KMR.strings.mod69_position_top),
                        "bottom" to stringResource(KMR.strings.mod69_position_bottom),
                    ),
                    title = stringResource(KMR.strings.mod69_info_bar_position),
                    enabled = infoBar,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69InfoBarTransparent(),
                    title = stringResource(KMR.strings.mod69_info_bar_transparent),
                    enabled = infoBar,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.mod69ClockFormat(),
                    entries = persistentMapOf(
                        "system" to stringResource(KMR.strings.mod69_clock_system),
                        "12" to stringResource(KMR.strings.mod69_clock_12),
                        "24" to stringResource(KMR.strings.mod69_clock_24),
                    ),
                    title = stringResource(KMR.strings.mod69_clock_format),
                ),
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(KMR.strings.mod69_reorder_buttons),
                ) {
                    val savedOrder by readerPreferences.mod69BottomButtonOrder().collectAsState()
                    val saved = savedOrder.split(",").filter { it.isNotBlank() }
                    val ordered = (saved + ReaderBottomButton.entries.map { it.value }).distinct()
                    val byKey = ReaderBottomButton.entries.associateBy { it.value }
                    Mod69ReorderPreferenceWidget(
                        title = stringResource(KMR.strings.mod69_reorder_buttons),
                        subtitle = stringResource(KMR.strings.mod69_reorder_buttons_summary),
                        items = ordered.mapNotNull { key -> byKey[key]?.let { key to stringResource(it.stringRes) } },
                        saveText = stringResource(MR.strings.action_save),
                        cancelText = stringResource(MR.strings.action_cancel),
                        onSave = { readerPreferences.mod69BottomButtonOrder().set(it.joinToString(",")) },
                    )
                },
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.mod69WebtoonGap(),
                    entries = persistentMapOf(
                        0 to "0",
                        5 to "5",
                        15 to "15 (normal)",
                        30 to "30",
                        60 to "60",
                    ),
                    title = stringResource(KMR.strings.mod69_webtoon_gap),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69ZoomButtons(),
                    title = stringResource(KMR.strings.mod69_zoom_buttons),
                    subtitle = stringResource(KMR.strings.mod69_zoom_buttons_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.mod69MaxZoom(),
                    entries = persistentMapOf(
                        5 to "5x",
                        8 to "8x",
                        12 to "12x",
                        20 to "20x",
                    ),
                    title = stringResource(KMR.strings.mod69_max_zoom),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69ReaderOwnWindow(),
                    title = stringResource(KMR.strings.mod69_reader_own_window),
                    subtitle = stringResource(KMR.strings.mod69_reader_own_window_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69AskPeek(),
                    title = stringResource(KMR.strings.mod69_ask_peek),
                    subtitle = stringResource(KMR.strings.mod69_ask_peek_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.mod69PageTransition(),
                    entries = persistentMapOf(
                        "none" to stringResource(KMR.strings.mod69_turn_slide),
                        "fade" to stringResource(KMR.strings.mod69_turn_fade),
                        "stack" to stringResource(KMR.strings.mod69_turn_stack),
                        "cube" to stringResource(KMR.strings.mod69_turn_cube),
                        "zoom" to stringResource(KMR.strings.mod69_turn_zoom),
                        "flip" to stringResource(KMR.strings.mod69_turn_flip),
                    ),
                    title = stringResource(KMR.strings.mod69_page_turn),
                ),
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(KMR.strings.mod69_tap_grid),
                ) {
                    eu.kanade.presentation.more.settings.widget.Mod69TapGridWidget(
                        title = stringResource(KMR.strings.mod69_tap_grid),
                        subtitle = stringResource(KMR.strings.mod69_tap_grid_summary),
                        preference = readerPreferences.mod69TapGrid(),
                        resetText = stringResource(KMR.strings.mod69_tap_grid_reset),
                    )
                },
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.mod69BarOpacity(),
                    entries = persistentMapOf(
                        0 to stringResource(KMR.strings.mod69_bar_opacity_normal),
                        75 to "75%",
                        50 to "50%",
                        25 to "25%",
                        10 to "10%",
                    ),
                    title = stringResource(KMR.strings.mod69_bar_opacity),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69FloatingMenuButton(),
                    title = stringResource(KMR.strings.mod69_floating_menu_button),
                    subtitle = stringResource(KMR.strings.mod69_floating_menu_button_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69SliderPreview(),
                    title = stringResource(KMR.strings.mod69_slider_preview),
                    subtitle = stringResource(KMR.strings.mod69_slider_preview_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.mod69AskLeavePages(),
                    entries = persistentMapOf(
                        0 to stringResource(KMR.strings.mod69_ask_leave_never),
                        3 to stringResource(KMR.strings.mod69_ask_leave_pages, 3),
                        10 to stringResource(KMR.strings.mod69_ask_leave_pages, 10),
                        20 to stringResource(KMR.strings.mod69_ask_leave_pages, 20),
                        50 to stringResource(KMR.strings.mod69_ask_leave_pages, 50),
                    ),
                    title = stringResource(KMR.strings.mod69_ask_leave),
                ),
            ),
        )
    }
    // KMK <--

    @Composable
    private fun getDisplayGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val fullscreenPref = readerPreferences.fullscreen()
        val fullscreen by fullscreenPref.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.defaultOrientationType(),
                    entries = ReaderOrientation.entries.drop(1)
                        .associate { it.flagValue to stringResource(it.stringRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_rotation_type),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.readerTheme(),
                    entries = persistentMapOf(
                        1 to stringResource(MR.strings.black_background),
                        2 to stringResource(MR.strings.gray_background),
                        0 to stringResource(MR.strings.white_background),
                        3 to stringResource(MR.strings.automatic_background),
                    ),
                    title = stringResource(MR.strings.pref_reader_theme),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = fullscreenPref,
                    title = stringResource(MR.strings.pref_fullscreen),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.drawUnderCutout(),
                    title = stringResource(MR.strings.pref_cutout_short),
                    enabled = LocalView.current.hasDisplayCutout() && fullscreen,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.keepScreenOn(),
                    title = stringResource(MR.strings.pref_keep_screen_on),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.showPageNumber(),
                    title = stringResource(MR.strings.pref_show_page_number),
                ),
            ),
        )
    }

    @Composable
    private fun getEInkGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val flashPageState by readerPreferences.flashOnPageChange().collectAsState()

        val flashMillisPref = readerPreferences.flashDurationMillis()
        val flashMillis by flashMillisPref.collectAsState()

        val flashIntervalPref = readerPreferences.flashPageInterval()
        val flashInterval by flashIntervalPref.collectAsState()

        val flashColorPref = readerPreferences.flashColor()

        return Preference.PreferenceGroup(
            title = "E-Ink",
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.flashOnPageChange(),
                    title = stringResource(MR.strings.pref_flash_page),
                    subtitle = stringResource(MR.strings.pref_flash_page_summ),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = flashMillis / ReaderPreferences.MILLI_CONVERSION,
                    valueRange = 1..15,
                    title = stringResource(MR.strings.pref_flash_duration),
                    valueString = stringResource(MR.strings.pref_flash_duration_summary, flashMillis),
                    enabled = flashPageState,
                    onValueChanged = { flashMillisPref.set(it * ReaderPreferences.MILLI_CONVERSION) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = flashInterval,
                    valueRange = 1..10,
                    title = stringResource(MR.strings.pref_flash_page_interval),
                    valueString = pluralStringResource(MR.plurals.pref_pages, flashInterval, flashInterval),
                    enabled = flashPageState,
                    onValueChanged = { flashIntervalPref.set(it) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = flashColorPref,
                    entries = persistentMapOf(
                        ReaderPreferences.FlashColor.BLACK to stringResource(MR.strings.pref_flash_style_black),
                        ReaderPreferences.FlashColor.WHITE to stringResource(MR.strings.pref_flash_style_white),
                        ReaderPreferences.FlashColor.WHITE_BLACK
                            to stringResource(MR.strings.pref_flash_style_white_black),
                    ),
                    title = stringResource(MR.strings.pref_flash_with),
                    enabled = flashPageState,
                ),
            ),
        )
    }

    @Composable
    private fun getReadingGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_reading),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.skipRead(),
                    title = stringResource(MR.strings.pref_skip_read_chapters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.skipFiltered(),
                    title = stringResource(MR.strings.pref_skip_filtered_chapters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.skipDupe(),
                    title = stringResource(MR.strings.pref_skip_dupe_chapters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.alwaysShowChapterTransition(),
                    title = stringResource(MR.strings.pref_always_show_chapter_transition),
                ),
            ),
        )
    }

    @Composable
    private fun getPagedGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val navModePref = readerPreferences.navigationModePager()
        val imageScaleTypePref = readerPreferences.imageScaleType()
        val dualPageSplitPref = readerPreferences.dualPageSplitPaged()
        val rotateToFitPref = readerPreferences.dualPageRotateToFit()

        val navMode by navModePref.collectAsState()
        val imageScaleType by imageScaleTypePref.collectAsState()
        val dualPageSplit by dualPageSplitPref.collectAsState()
        val rotateToFit by rotateToFitPref.collectAsState()

        // KMK -->
        val pagedDisableZoomInPref = readerPreferences.pagedDisableZoomIn()
        val landscapeZoomPref = readerPreferences.landscapeZoom()
        val pagedDisableZoomIn by pagedDisableZoomInPref.collectAsState()
        val landscapeZoom by landscapeZoomPref.collectAsState()
        // KMK <--

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pager_viewer),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = navModePref,
                    entries = ReaderPreferences.TapZones
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_viewer_nav),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.pagerNavInverted(),
                    entries = persistentListOf(
                        ReaderPreferences.TappingInvertMode.NONE,
                        ReaderPreferences.TappingInvertMode.HORIZONTAL,
                        ReaderPreferences.TappingInvertMode.VERTICAL,
                        ReaderPreferences.TappingInvertMode.BOTH,
                    )
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                    enabled = navMode != 5,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = imageScaleTypePref,
                    entries = ReaderPreferences.ImageScaleType
                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_image_scale_type),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.zoomStart(),
                    entries = ReaderPreferences.ZoomStart
                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_zoom_start),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.cropBorders(),
                    title = stringResource(MR.strings.pref_crop_borders),
                ),
                // SY -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.pageTransitionsPager(),
                    title = stringResource(MR.strings.pref_page_transitions),
                ),
                // SY <--
                Preference.PreferenceItem.SwitchPreference(
                    preference = landscapeZoomPref,
                    title = stringResource(MR.strings.pref_landscape_zoom),
                    // KMK -->
                    enabled = imageScaleType in zoomWideImagesAllowedList,
                    // KMK <--
                ),
                // KMK -->
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.landscapeZoomType(),
                    entries = ReaderPreferences.LandscapeZoomScaleType.entries
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    title = stringResource(KMR.strings.pref_landscape_zoom_type),
                    enabled = landscapeZoom && imageScaleType in zoomWideImagesAllowedList,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = pagedDisableZoomInPref,
                    title = stringResource(KMR.strings.pref_paged_disable_zoom_in),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.pagedDoubleTapZoomEnabled(),
                    title = stringResource(MR.strings.pref_double_tap_zoom),
                    enabled = !pagedDisableZoomIn,
                ),
                // KMK <--
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.navigateToPan(),
                    title = stringResource(MR.strings.pref_navigate_pan),
                    enabled = navMode != 5,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = dualPageSplitPref,
                    title = stringResource(MR.strings.pref_dual_page_split),
                    onValueChanged = {
                        rotateToFitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.dualPageInvertPaged(),
                    title = stringResource(MR.strings.pref_dual_page_invert),
                    subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
                    enabled = dualPageSplit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = rotateToFitPref,
                    title = stringResource(MR.strings.pref_page_rotate),
                    onValueChanged = {
                        dualPageSplitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.dualPageRotateToFitInvert(),
                    title = stringResource(MR.strings.pref_page_rotate_invert),
                    enabled = rotateToFit,
                ),
            ),
        )
    }

    @Composable
    private fun getWebtoonGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val numberFormat = remember { NumberFormat.getPercentInstance() }

        val navModePref = readerPreferences.navigationModeWebtoon()
        val dualPageSplitPref = readerPreferences.dualPageSplitWebtoon()
        val rotateToFitPref = readerPreferences.dualPageRotateToFitWebtoon()
        val webtoonSidePaddingPref = readerPreferences.webtoonSidePadding()

        val navMode by navModePref.collectAsState()
        val dualPageSplit by dualPageSplitPref.collectAsState()
        val rotateToFit by rotateToFitPref.collectAsState()
        val webtoonSidePadding by webtoonSidePaddingPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.webtoon_viewer),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = navModePref,
                    entries = ReaderPreferences.TapZones
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_viewer_nav),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.webtoonNavInverted(),
                    entries = persistentListOf(
                        ReaderPreferences.TappingInvertMode.NONE,
                        ReaderPreferences.TappingInvertMode.HORIZONTAL,
                        ReaderPreferences.TappingInvertMode.VERTICAL,
                        ReaderPreferences.TappingInvertMode.BOTH,
                    )
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                    enabled = navMode != 5,
                ),
                // KMK -->
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.webtoonScaleType(),
                    entries = WebtoonScaleType.entries
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    title = stringResource(KMR.strings.pref_webtoon_scale_type),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.longStripGapSmartScale(),
                    title = stringResource(KMR.strings.pref_smart_scale_long_strip_gap),
                ),
                // KMK <--
                Preference.PreferenceItem.SliderPreference(
                    value = webtoonSidePadding,
                    valueRange = ReaderPreferences.let {
                        it.WEBTOON_PADDING_MIN..it.WEBTOON_PADDING_MAX
                    },
                    title = stringResource(MR.strings.pref_webtoon_side_padding),
                    valueString = numberFormat.format(webtoonSidePadding / 100f),
                    onValueChanged = { webtoonSidePaddingPref.set(it) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.readerHideThreshold(),
                    entries = persistentMapOf(
                        ReaderPreferences.ReaderHideThreshold.HIGHEST to stringResource(MR.strings.pref_highest),
                        ReaderPreferences.ReaderHideThreshold.HIGH to stringResource(MR.strings.pref_high),
                        ReaderPreferences.ReaderHideThreshold.LOW to stringResource(MR.strings.pref_low),
                        ReaderPreferences.ReaderHideThreshold.LOWEST to stringResource(MR.strings.pref_lowest),
                    ),
                    title = stringResource(MR.strings.pref_hide_threshold),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.cropBordersWebtoon(),
                    title = stringResource(MR.strings.pref_crop_borders),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = dualPageSplitPref,
                    title = stringResource(MR.strings.pref_dual_page_split),
                    onValueChanged = {
                        rotateToFitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.dualPageInvertWebtoon(),
                    title = stringResource(MR.strings.pref_dual_page_invert),
                    subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
                    enabled = dualPageSplit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = rotateToFitPref,
                    title = stringResource(MR.strings.pref_page_rotate),
                    onValueChanged = {
                        dualPageSplitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.dualPageRotateToFitInvertWebtoon(),
                    title = stringResource(MR.strings.pref_page_rotate_invert),
                    enabled = rotateToFit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.webtoonDoubleTapZoomEnabled(),
                    title = stringResource(MR.strings.pref_double_tap_zoom),
                ),
                // KMK -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.webtoonPinchToZoomEnabled(),
                    title = stringResource(KMR.strings.pref_pinch_to_zoom),
                ),
                // KMK <--
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.webtoonDisableZoomOut(),
                    title = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
                ),
                // SY -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.pageTransitionsWebtoon(),
                    title = stringResource(MR.strings.pref_page_transitions),
                ),
                // SY <--
            ),
        )
    }

    // SY -->
    @Composable
    private fun getContinuousVerticalGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.vertical_plus_viewer),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.continuousVerticalTappingByPage(),
                    title = stringResource(SYMR.strings.tap_scroll_page),
                    subtitle = stringResource(SYMR.strings.tap_scroll_page_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.cropBordersContinuousVertical(),
                    title = stringResource(MR.strings.pref_crop_borders),
                ),
            ),
        )
    }
    // SY <--

    @Composable
    private fun getNavigationGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val readWithVolumeKeysPref = readerPreferences.readWithVolumeKeys()
        val readWithVolumeKeys by readWithVolumeKeysPref.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_navigation),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readWithVolumeKeysPref,
                    title = stringResource(MR.strings.pref_read_with_volume_keys),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.readWithVolumeKeysInverted(),
                    title = stringResource(MR.strings.pref_read_with_volume_keys_inverted),
                    enabled = readWithVolumeKeys,
                ),
            ),
        )
    }

    @Composable
    private fun getActionsGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_actions),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.readWithLongTap(),
                    title = stringResource(MR.strings.pref_read_with_long_tap),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.folderPerManga(),
                    title = stringResource(MR.strings.pref_create_folder_per_manga),
                    subtitle = stringResource(MR.strings.pref_create_folder_per_manga_summary),
                ),
            ),
        )
    }

    // SY -->
    @Composable
    private fun getPageDownloadingGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(SYMR.strings.page_downloading),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.preloadSize(),
                    entries = persistentMapOf(
                        4 to stringResource(SYMR.strings.reader_preload_amount_4_pages),
                        6 to stringResource(SYMR.strings.reader_preload_amount_6_pages),
                        8 to stringResource(SYMR.strings.reader_preload_amount_8_pages),
                        10 to stringResource(SYMR.strings.reader_preload_amount_10_pages),
                        12 to stringResource(SYMR.strings.reader_preload_amount_12_pages),
                        14 to stringResource(SYMR.strings.reader_preload_amount_14_pages),
                        16 to stringResource(SYMR.strings.reader_preload_amount_16_pages),
                        20 to stringResource(SYMR.strings.reader_preload_amount_20_pages),
                    ),
                    title = stringResource(SYMR.strings.reader_preload_amount),
                    subtitle = stringResource(SYMR.strings.reader_preload_amount_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.readerThreads(),
                    title = stringResource(SYMR.strings.download_threads),
                    subtitle = stringResource(SYMR.strings.download_threads_summary),
                    entries = List(5) { it }.associateWith { it.toString() }.toImmutableMap(),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.cacheSize(),
                    title = stringResource(SYMR.strings.reader_cache_size),
                    subtitle = stringResource(SYMR.strings.reader_cache_size_summary),
                    entries = persistentMapOf(
                        "50" to "50 MB",
                        "75" to "75 MB",
                        "100" to "100 MB",
                        "150" to "150 MB",
                        "250" to "250 MB",
                        "500" to "500 MB",
                        "750" to "750 MB",
                        "1000" to "1 GB",
                        "1500" to "1.5 GB",
                        "2000" to "2 GB",
                        "2500" to "2.5 GB",
                        "3000" to "3 GB",
                        "3500" to "3.5 GB",
                        "4000" to "4 GB",
                        "4500" to "4.5 GB",
                        "5000" to "5 GB",
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.aggressivePageLoading(),
                    title = stringResource(SYMR.strings.aggressively_load_pages),
                    subtitle = stringResource(SYMR.strings.aggressively_load_pages_summary),
                ),
                // KMK --> 69 mod
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.mod69LowMemory(),
                    title = stringResource(KMR.strings.mod69_low_memory),
                    subtitle = stringResource(KMR.strings.mod69_low_memory_summary),
                ),
                // KMK <--
            ),
        )
    }

    @Composable
    private fun getForkSettingsGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val pageLayout by readerPreferences.pageLayout().collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(SYMR.strings.pref_category_fork),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.readerInstantRetry(),
                    title = stringResource(SYMR.strings.skip_queue_on_retry),
                    subtitle = stringResource(SYMR.strings.skip_queue_on_retry_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.preserveReadingPosition(),
                    title = stringResource(SYMR.strings.preserve_reading_position),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.useAutoWebtoon(),
                    title = stringResource(SYMR.strings.auto_webtoon_mode),
                    subtitle = stringResource(SYMR.strings.auto_webtoon_mode_summary),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = readerPreferences.readerBottomButtons(),
                    entries = ReaderBottomButton.entries
                        .associate { it.value to stringResource(it.stringRes) }
                        .toImmutableMap(),
                    title = stringResource(SYMR.strings.reader_bottom_buttons),
                    subtitle = stringResource(SYMR.strings.reader_bottom_buttons_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.pageLayout(),
                    entries = ReaderPreferences.PageLayouts
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                    title = stringResource(SYMR.strings.page_layout),
                    subtitle = stringResource(SYMR.strings.automatic_can_still_switch),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.invertDoublePages(),
                    title = stringResource(SYMR.strings.invert_double_pages),
                    enabled = pageLayout != PagerConfig.PageLayout.SINGLE_PAGE,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.centerMarginType(),
                    entries = ReaderPreferences.CenterMarginTypes
                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                    title = stringResource(SYMR.strings.center_margin),
                    subtitle = stringResource(SYMR.strings.pref_center_margin_summary),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.archiveReaderMode(),
                    entries = ReaderPreferences.archiveModeTypes
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap()
                        .toImmutableMap(),
                    title = stringResource(SYMR.strings.pref_archive_reader_mode),
                    subtitle = stringResource(SYMR.strings.pref_archive_reader_mode_summary),
                ),
            ),
        )
    }
    // SY <--
}
