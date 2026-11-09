package eu.kanade.tachiyomi.ui.reader.setting

import android.os.Build
import androidx.compose.ui.graphics.BlendMode
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR

class ReaderPreferences(
    private val preferenceStore: PreferenceStore,
) {

    // region General

    // SY -->
    fun pageTransitionsPager() = preferenceStore.getBoolean("pref_enable_transitions_pager_key", true)

    fun pageTransitionsWebtoon() = preferenceStore.getBoolean("pref_enable_transitions_webtoon_key", true)
    // SY <--

    fun flashOnPageChange() = preferenceStore.getBoolean("pref_reader_flash", false)

    fun flashDurationMillis() = preferenceStore.getInt("pref_reader_flash_duration", MILLI_CONVERSION)

    fun flashPageInterval() = preferenceStore.getInt("pref_reader_flash_interval", 1)

    fun flashColor() = preferenceStore.getEnum("pref_reader_flash_mode", FlashColor.BLACK)

    fun doubleTapAnimSpeed() = preferenceStore.getInt("pref_double_tap_anim_speed", 500)

    fun showPageNumber() = preferenceStore.getBoolean("pref_show_page_number_key", true)

    fun showReadingMode() = preferenceStore.getBoolean("pref_show_reading_mode", true)

    fun fullscreen() = preferenceStore.getBoolean("fullscreen", true)

    fun drawUnderCutout() = preferenceStore.getBoolean("cutout_short", true)

    fun keepScreenOn() = preferenceStore.getBoolean("pref_keep_screen_on_key", false)

    fun defaultReadingMode() = preferenceStore.getInt(
        "pref_default_reading_mode_key",
        ReadingMode.RIGHT_TO_LEFT.flagValue,
    )

    fun defaultOrientationType() = preferenceStore.getInt(
        "pref_default_orientation_type_key",
        ReaderOrientation.FREE.flagValue,
    )

    fun webtoonDoubleTapZoomEnabled() = preferenceStore.getBoolean("pref_enable_double_tap_zoom_webtoon", true)

    // KMK -->
    fun pagedDoubleTapZoomEnabled() = preferenceStore.getBoolean("pref_enable_double_tap_zoom_paged", true)

    fun webtoonPinchToZoomEnabled() = preferenceStore.getBoolean("pref_enable_pinch_to_zoom_webtoon", true)
    // KMK <--

    fun imageScaleType() = preferenceStore.getInt("pref_image_scale_type_key", 1)

    // KMK -->
    fun webtoonScaleType() = preferenceStore.getEnum("pref_webtoon_scale_type_key", WebtoonScaleType.FIT)
    fun longStripGapSmartScale() = preferenceStore.getBoolean("pref_webtoon_smart_scale_long_strip_gap", false)
    // KMK <--

    fun zoomStart() = preferenceStore.getInt("pref_zoom_start_key", 1)

    fun readerTheme() = preferenceStore.getInt("pref_reader_theme_key", 1)

    fun alwaysShowChapterTransition() = preferenceStore.getBoolean("always_show_chapter_transition", true)

    fun cropBorders() = preferenceStore.getBoolean("crop_borders", false)

    fun navigateToPan() = preferenceStore.getBoolean("navigate_pan", true)

    fun landscapeZoom() = preferenceStore.getBoolean("landscape_zoom", true)

    // KMK -->
    fun landscapeZoomType() = preferenceStore.getEnum("landscape_zoom_type", LandscapeZoomScaleType.FIT)
    // KMK <--

    fun cropBordersWebtoon() = preferenceStore.getBoolean("crop_borders_webtoon", false)

    fun webtoonSidePadding() = preferenceStore.getInt("webtoon_side_padding", WEBTOON_PADDING_MIN)

    fun readerHideThreshold() = preferenceStore.getEnum("reader_hide_threshold", ReaderHideThreshold.LOW)

    fun folderPerManga() = preferenceStore.getBoolean("create_folder_per_manga", false)

    fun skipRead() = preferenceStore.getBoolean("skip_read", false)

    fun skipFiltered() = preferenceStore.getBoolean("skip_filtered", true)

    fun skipDupe() = preferenceStore.getBoolean("skip_dupe", false)

    fun webtoonDisableZoomOut() = preferenceStore.getBoolean("webtoon_disable_zoom_out", false)

    // KMK -->
    fun pagedDisableZoomIn() = preferenceStore.getBoolean("paged_disable_zoom_in", false)
    // KMK <--

    // endregion

    // region Split two page spread

    fun dualPageSplitPaged() = preferenceStore.getBoolean("pref_dual_page_split", false)

    fun dualPageInvertPaged() = preferenceStore.getBoolean("pref_dual_page_invert", false)

    fun dualPageSplitWebtoon() = preferenceStore.getBoolean("pref_dual_page_split_webtoon", false)

    fun dualPageInvertWebtoon() = preferenceStore.getBoolean("pref_dual_page_invert_webtoon", false)

    fun dualPageRotateToFit() = preferenceStore.getBoolean("pref_dual_page_rotate", false)

    fun dualPageRotateToFitInvert() = preferenceStore.getBoolean("pref_dual_page_rotate_invert", false)

    fun dualPageRotateToFitWebtoon() = preferenceStore.getBoolean("pref_dual_page_rotate_webtoon", false)

    fun dualPageRotateToFitInvertWebtoon() = preferenceStore.getBoolean("pref_dual_page_rotate_invert_webtoon", false)

    // endregion

    // region Color filter

    fun customBrightness() = preferenceStore.getBoolean("pref_custom_brightness_key", false)

    fun customBrightnessValue() = preferenceStore.getInt("custom_brightness_value", 0)

    fun colorFilter() = preferenceStore.getBoolean("pref_color_filter_key", false)

    fun colorFilterValue() = preferenceStore.getInt("color_filter_value", 0)

    fun colorFilterMode() = preferenceStore.getInt("color_filter_mode", 0)

    fun grayscale() = preferenceStore.getBoolean("pref_grayscale", false)

    fun invertedColors() = preferenceStore.getBoolean("pref_inverted_colors", false)

    // endregion

    // region Controls

    fun readWithLongTap() = preferenceStore.getBoolean("reader_long_tap", true)

    fun readWithVolumeKeys() = preferenceStore.getBoolean("reader_volume_keys", false)

    fun readWithVolumeKeysInverted() = preferenceStore.getBoolean("reader_volume_keys_inverted", false)

    fun navigationModePager() = preferenceStore.getInt("reader_navigation_mode_pager", 0)

    fun navigationModeWebtoon() = preferenceStore.getInt("reader_navigation_mode_webtoon", 0)

    fun pagerNavInverted() = preferenceStore.getEnum("reader_tapping_inverted", TappingInvertMode.NONE)

    fun webtoonNavInverted() = preferenceStore.getEnum("reader_tapping_inverted_webtoon", TappingInvertMode.NONE)

    fun showNavigationOverlayNewUser() = preferenceStore.getBoolean("reader_navigation_overlay_new_user", true)

    fun showNavigationOverlayOnStart() = preferenceStore.getBoolean("reader_navigation_overlay_on_start", false)

    // KMK -->
    fun smallerTapZone() = preferenceStore.getBoolean("reader_navigation_smaller_tap_zone", false)
    // KMK <--

    // endregion

    // SY -->

    fun readerThreads() = preferenceStore.getInt("eh_reader_threads", 2)

    fun readerInstantRetry() = preferenceStore.getBoolean("eh_reader_instant_retry", true)

    fun aggressivePageLoading() = preferenceStore.getBoolean("eh_aggressive_page_loading", false)

    fun cacheSize() = preferenceStore.getString("eh_cache_size", "75")

    fun autoscrollInterval() = preferenceStore.getFloat("eh_util_autoscroll_interval", 3f)

    fun smoothAutoScroll() = preferenceStore.getBoolean("smooth_auto_scroll", true)

    fun preserveReadingPosition() = preferenceStore.getBoolean("eh_preserve_reading_position", false)

    fun preloadSize() = preferenceStore.getInt("eh_preload_size", 10)

    // KMK --> 69 mod
    /**
     * Low memory mode for weak phones: fewer pages kept and preloaded, one loading thread,
     * pages you've scrolled past are let go sooner, and lighter tiles when zooming.
     */
    fun mod69LowMemory() = preferenceStore.getBoolean("mod69_low_memory_reader", false)

    /** R5: avoid the camera notch only when the phone is held sideways. */
    fun mod69CutoutAvoidSideways() = preferenceStore.getBoolean("mod69_cutout_avoid_sideways", false)

    /** R6: height of the vertical page slider, 30..100 percent of the screen. */
    fun mod69SeekbarHeight() = preferenceStore.getInt("mod69_seekbar_height", 100)

    /** R8: on mobile data only the next 2 pages are loaded in advance. */
    fun mod69PreloadWifiOnly() = preferenceStore.getBoolean("mod69_preload_wifi_only", false)

    /** R12: small line with time, battery, chapter and page. */
    fun mod69InfoBar() = preferenceStore.getBoolean("mod69_info_bar", false)

    fun mod69InfoBarClock() = preferenceStore.getBoolean("mod69_info_bar_clock", true)

    fun mod69InfoBarBattery() = preferenceStore.getBoolean("mod69_info_bar_battery", true)

    fun mod69InfoBarChapter() = preferenceStore.getBoolean("mod69_info_bar_chapter", true)

    fun mod69InfoBarPage() = preferenceStore.getBoolean("mod69_info_bar_page", true)

    /** "top" or "bottom". */
    fun mod69InfoBarPosition() = preferenceStore.getString("mod69_info_bar_position", "top")

    /** 0 = solid, 100 = see-through. */
    fun mod69InfoBarTransparent() = preferenceStore.getBoolean("mod69_info_bar_transparent", true)

    /** U9: "system", "12" or "24" hour clock (reader info bar and elsewhere). */
    fun mod69ClockFormat() = preferenceStore.getString("mod69_clock_format", "system")

    /** R10: gap between long-strip pages in dp. */
    fun mod69WebtoonGap() = preferenceStore.getInt("mod69_webtoon_gap", 15)

    /** P4: order of the buttons in the reader's bottom bar (comma separated keys). */
    fun mod69BottomButtonOrder() = preferenceStore.getString("mod69_bottom_button_order", "")

    /** R14: on-screen + and - zoom buttons. */
    fun mod69ZoomButtons() = preferenceStore.getBoolean("mod69_zoom_buttons", false)

    /** R20: the reader opens as its own window in the recent apps list. */
    fun mod69ReaderOwnWindow() = preferenceStore.getBoolean("mod69_reader_own_window", false)

    /** R24: opening a chapter you already read asks: peek or move progress. */
    fun mod69AskPeek() = preferenceStore.getBoolean("mod69_ask_peek", false)

    /** N20: maximum zoom multiplier (5 = default). */
    fun mod69MaxZoom() = preferenceStore.getInt("mod69_max_zoom", 5)

    /** P5: pressing back after this many pages asks before closing the reader. 0 = never ask. */
    fun mod69AskLeavePages() = preferenceStore.getInt("mod69_ask_leave_pages", 0)

    /** R2: picture filter on pages: none, sharpen, strong, smooth. */
    fun mod69PageFilter() = preferenceStore.getString("mod69_page_filter", "none")

    /** R22: taps move from panel to panel before turning the page (page-by-page modes). */
    fun mod69PanelMode() = preferenceStore.getBoolean("mod69_panel_mode", false)

    /** N2: pages show a blurry first look while downloading and sharpen as they arrive. */
    fun mod69ProgressivePages() = preferenceStore.getBoolean("mod69_progressive_pages", false)

    /** R1: page turn animation for page-by-page reading: none, fade, stack, cube, zoom, flip. */
    fun mod69PageTransition() = preferenceStore.getString("mod69_page_transition", "none")

    /** R16: the user's own tap zones (3 x 3 letters, see Mod69CustomNavigation). */
    fun mod69TapGrid() = preferenceStore.getString("mod69_tap_grid", "PPPPMNNNN")

    /** R15: how solid the reader's top and bottom bars are, in percent. 0 = normal. */
    fun mod69BarOpacity() = preferenceStore.getInt("mod69_bar_opacity", 0)

    /** R15: a small round button that shows / hides the reader menu. */
    fun mod69FloatingMenuButton() = preferenceStore.getBoolean("mod69_floating_menu_button", false)

    /** N23: show a small picture of the page while dragging the page slider. */
    fun mod69SliderPreview() = preferenceStore.getBoolean("mod69_slider_preview", true)
    // KMK <--

    fun useAutoWebtoon() = preferenceStore.getBoolean("eh_use_auto_webtoon", true)

    fun continuousVerticalTappingByPage() = preferenceStore.getBoolean("continuous_vertical_tapping_by_page", false)

    fun cropBordersContinuousVertical() = preferenceStore.getBoolean("crop_borders_continues_vertical", false)

    fun forceHorizontalSeekbar() = preferenceStore.getBoolean("pref_force_horz_seekbar", false)

    fun landscapeVerticalSeekbar() = preferenceStore.getBoolean("pref_show_vert_seekbar_landscape", false)

    fun leftVerticalSeekbar() = preferenceStore.getBoolean("pref_left_handed_vertical_seekbar", false)

    fun readerBottomButtons() = preferenceStore.getStringSet("reader_bottom_buttons", ReaderBottomButton.BUTTONS_DEFAULTS)

    fun pageLayout() = preferenceStore.getInt("page_layout", PagerConfig.PageLayout.AUTOMATIC)

    fun invertDoublePages() = preferenceStore.getBoolean("invert_double_pages", false)

    fun centerMarginType() = preferenceStore.getInt("center_margin_type", PagerConfig.CenterMarginType.NONE)

    fun archiveReaderMode() = preferenceStore.getInt("archive_reader_mode", ArchiveReaderMode.LOAD_FROM_FILE)
    // SY <--

    enum class FlashColor {
        BLACK,
        WHITE,
        WHITE_BLACK,
    }

    enum class TappingInvertMode(
        val titleRes: StringResource,
        val shouldInvertHorizontal: Boolean = false,
        val shouldInvertVertical: Boolean = false,
    ) {
        NONE(MR.strings.tapping_inverted_none),
        HORIZONTAL(MR.strings.tapping_inverted_horizontal, shouldInvertHorizontal = true),
        VERTICAL(MR.strings.tapping_inverted_vertical, shouldInvertVertical = true),
        BOTH(MR.strings.tapping_inverted_both, shouldInvertHorizontal = true, shouldInvertVertical = true),
    }

    enum class ReaderHideThreshold(val threshold: Int) {
        HIGHEST(5),
        HIGH(13),
        LOW(31),
        LOWEST(47),
    }

    // KMK -->
    enum class LandscapeZoomScaleType(
        val titleRes: StringResource,
    ) {
        FIT(MR.strings.scale_type_fit_screen),
        DOUBLE(KMR.strings.scale_type_double),
    }

    enum class WebtoonScaleType(
        val titleRes: StringResource,
        val ratio: Float,
    ) {
        FIT(MR.strings.scale_type_fit_screen, 0f),
        R4_3(KMR.strings.scale_type_4_3, 3f / 4f),
        R3_2(KMR.strings.scale_type_3_2, 2f / 3f),
        R16_9(KMR.strings.scale_type_16_9, 9f / 16f),
        R20_9(KMR.strings.scale_type_20_9, 9f / 20f),
    }
    // KMK <--

    object ArchiveReaderMode {
        const val LOAD_FROM_FILE = 0
        const val LOAD_INTO_MEMORY = 1
        const val CACHE_TO_DISK = 2
    }

    companion object {
        const val WEBTOON_PADDING_MIN = 0
        const val WEBTOON_PADDING_MAX = 25

        const val MILLI_CONVERSION = 100

        val TapZones = listOf(
            MR.strings.label_default,
            MR.strings.l_nav,
            MR.strings.kindlish_nav,
            MR.strings.edge_nav,
            MR.strings.right_and_left_nav,
            MR.strings.disabled_nav,
            // KMK -->
            KMR.strings.mod69_custom_nav,
            // KMK <--
        )

        val ImageScaleType = listOf(
            MR.strings.scale_type_fit_screen,
            MR.strings.scale_type_stretch,
            MR.strings.scale_type_fit_width,
            MR.strings.scale_type_fit_height,
            MR.strings.scale_type_original_size,
            MR.strings.scale_type_smart_fit,
        )

        val ZoomStart = listOf(
            MR.strings.zoom_start_automatic,
            MR.strings.zoom_start_left,
            MR.strings.zoom_start_right,
            MR.strings.zoom_start_center,
        )

        // KMK -->
        val zoomWideImagesAllowedList = listOf(
            SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE,
            SubsamplingScaleImageView.SCALE_TYPE_ORIGINAL_SIZE,
        )
        // KMK <--

        val ColorFilterMode = buildList {
            addAll(
                listOf(
                    MR.strings.label_default to BlendMode.SrcOver,
                    MR.strings.filter_mode_multiply to BlendMode.Modulate,
                    MR.strings.filter_mode_screen to BlendMode.Screen,
                ),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addAll(
                    listOf(
                        MR.strings.filter_mode_overlay to BlendMode.Overlay,
                        MR.strings.filter_mode_lighten to BlendMode.Lighten,
                        MR.strings.filter_mode_darken to BlendMode.Darken,
                    ),
                )
            }
        }

        // SY -->
        val PageLayouts = listOf(
            SYMR.strings.single_page,
            SYMR.strings.double_pages,
            SYMR.strings.automatic_orientation,
        )

        val CenterMarginTypes = listOf(
            SYMR.strings.center_margin_none,
            SYMR.strings.center_margin_double_page,
            SYMR.strings.center_margin_wide_page,
            SYMR.strings.center_margin_double_and_wide_page,
        )

        val archiveModeTypes = listOf(
            SYMR.strings.archive_mode_load_from_file,
            SYMR.strings.archive_mode_load_into_memory,
            SYMR.strings.archive_mode_cache_to_disk,
        )
        // SY <--
    }
}
