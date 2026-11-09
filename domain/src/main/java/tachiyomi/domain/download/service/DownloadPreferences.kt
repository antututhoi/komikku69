package tachiyomi.domain.download.service

import tachiyomi.core.common.preference.PreferenceStore

class DownloadPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun downloadOnlyOverWifi() = preferenceStore.getBoolean(
        "pref_download_only_over_wifi_key",
        true,
    )

    fun saveChaptersAsCBZ() = preferenceStore.getBoolean("save_chapter_as_cbz", true)

    fun splitTallImages() = preferenceStore.getBoolean("split_tall_images", true)

    // KMK --> 69 mod: page quality of downloads
    fun mod69PageQuality() = preferenceStore.getString("mod69_page_quality", "original")

    // typed in as numbers (sliders were not exact)
    fun mod69PageMaxWidthText() = preferenceStore.getString("mod69_page_max_width_txt", "1080")

    fun mod69PageMaxHeightText() = preferenceStore.getString("mod69_page_max_height_txt", "0")

    fun mod69PageWebpQualityText() = preferenceStore.getString("mod69_page_webp_quality_txt", "80")

    /** Max page width in px, 0 = no limit. */
    fun mod69PageMaxWidth(): Int = mod69PageMaxWidthText().get().trim().toIntOrNull()?.coerceAtLeast(0) ?: 1080

    /** Max page height in px, 0 = no limit. */
    fun mod69PageMaxHeight(): Int = mod69PageMaxHeightText().get().trim().toIntOrNull()?.coerceAtLeast(0) ?: 0

    /** Long webtoon strips (taller than 2.5x their width) only get the width limit, so they stay readable. */
    fun mod69KeepTallPages() = preferenceStore.getBoolean("mod69_keep_tall_pages", true)

    /** Category names (comma separated) never used as a download folder; each also matches names starting with it. */
    fun mod69ExcludedFolderCategories() = preferenceStore.getString("mod69_excluded_folder_categories", "")

    /** Migrating an entry moves its downloaded chapters to the new entry instead of deleting them. */
    fun mod69MigrateMoveDownloads() = preferenceStore.getBoolean("mod69_migrate_move_downloads", true)

    /** Entries (manga ids) whose downloads are never made smaller. */
    fun mod69NoResizeEntries() = preferenceStore.getStringSet("mod69_no_resize_entries", emptySet())

    /** New downloads go into a folder named after the entry's category instead of the source. */
    fun mod69CategoryFolders() = preferenceStore.getBoolean("mod69_category_folders", false)

    /** Chapter folder names start with the scanlator's name ("Group_Chapter 1"). Off = just "Chapter 1". */
    fun mod69ScanlatorInChapterNames() = preferenceStore.getBoolean("mod69_scanlator_in_chapter_names", false)

    /** Entry and chapter folder names are cut to [mod69ShortNameLength] characters (Windows-safe). */
    fun mod69ShortNames() = preferenceStore.getBoolean("mod69_short_names", false)

    fun mod69ShortNameLengthText() = preferenceStore.getString("mod69_short_name_length", "80")

    /** Max characters of an entry / chapter folder name when [mod69ShortNames] is on (30..200). */
    fun mod69ShortNameLength(): Int = mod69ShortNameLengthText().get().trim().toIntOrNull()?.coerceIn(30, 200) ?: 80

    /** W4: pause in milliseconds before each picture is requested (0 = none). */
    fun mod69DownloadDelayMs() = preferenceStore.getInt("mod69_download_delay_ms", 0)

    /** W4: how many times a picture is tried again after a failure (3 = normal). */
    fun mod69DownloadRetries() = preferenceStore.getInt("mod69_download_retries", 3)

    /** P17: speed limit in KB per second for all downloads together (0 = no limit). */
    fun mod69SpeedLimitKb() = preferenceStore.getInt("mod69_speed_limit_kb", 0)

    /** WebP quality 1..100. */
    fun mod69PageWebpQuality(): Int = mod69PageWebpQualityText().get().trim().toIntOrNull()?.coerceIn(1, 100) ?: 80
    // KMK <--

    fun autoDownloadWhileReading() = preferenceStore.getInt("auto_download_while_reading", 0)

    fun removeAfterReadSlots() = preferenceStore.getInt("remove_after_read_slots", -1)

    fun removeAfterMarkedAsRead() = preferenceStore.getBoolean(
        "pref_remove_after_marked_as_read_key",
        false,
    )

    fun removeBookmarkedChapters() = preferenceStore.getBoolean("pref_remove_bookmarked", false)

    fun removeExcludeCategories() = preferenceStore.getStringSet(REMOVE_EXCLUDE_CATEGORIES_PREF_KEY, emptySet())

    fun downloadNewChapters() = preferenceStore.getBoolean("download_new", false)

    fun downloadNewChapterCategories() = preferenceStore.getStringSet(DOWNLOAD_NEW_CATEGORIES_PREF_KEY, emptySet())

    fun downloadNewChapterCategoriesExclude() =
        preferenceStore.getStringSet(DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY, emptySet())

    fun downloadNewUnreadChaptersOnly() = preferenceStore.getBoolean("download_new_unread_chapters_only", false)

    fun parallelSourceLimit() = preferenceStore.getInt("download_parallel_source_limit", 5)

    fun parallelPageLimit() = preferenceStore.getInt("download_parallel_page_limit", 5)

    // SY -->
    fun includeChapterUrlHash() = preferenceStore.getBoolean("download_include_chapter_url_hash", false)
    // SY <--

    // KMK -->
    fun downloadCacheRenewInterval() = preferenceStore.getInt("download_cache_renew_interval", 1)
    // KMK <--

    companion object {
        private const val REMOVE_EXCLUDE_CATEGORIES_PREF_KEY = "remove_exclude_categories"
        private const val DOWNLOAD_NEW_CATEGORIES_PREF_KEY = "download_new_categories"
        private const val DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY = "download_new_categories_exclude"
        val categoryPreferenceKeys = setOf(
            REMOVE_EXCLUDE_CATEGORIES_PREF_KEY,
            DOWNLOAD_NEW_CATEGORIES_PREF_KEY,
            DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY,
        )
    }
}
