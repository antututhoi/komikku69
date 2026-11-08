package eu.kanade.presentation.more.settings.widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.materialkolor.PaletteStyle
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.util.system.toast
import org.json.JSONObject
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource

// KMK -->
private data class Mod69ThemePreset(val name: String, val seed: Long, val style: PaletteStyle)

private val presets = listOf(
    Mod69ThemePreset("Tokyo Night", 0xFF7AA2F7, PaletteStyle.TonalSpot),
    Mod69ThemePreset("Spring Blossom", 0xFFF4A7C0, PaletteStyle.Expressive),
    Mod69ThemePreset("Chocolate Strawberries", 0xFFB5384F, PaletteStyle.Vibrant),
    Mod69ThemePreset("Lime Time", 0xFFA3E635, PaletteStyle.Vibrant),
    Mod69ThemePreset("Pure White", 0xFFFFFFFF, PaletteStyle.Monochrome),
    Mod69ThemePreset("Miku", 0xFF39C5BB, PaletteStyle.Vibrant),
    Mod69ThemePreset("Sakura", 0xFFFFB7C5, PaletteStyle.TonalSpot),
    Mod69ThemePreset("Totoro", 0xFF7E9C6B, PaletteStyle.TonalSpot),
    Mod69ThemePreset("Amber", 0xFFFFB300, PaletteStyle.Vibrant),
    Mod69ThemePreset("Sky", 0xFF4FC3F7, PaletteStyle.TonalSpot),
    Mod69ThemePreset("Midnight Purple", 0xFF6A4CD6, PaletteStyle.Expressive),
    Mod69ThemePreset("Sunset", 0xFFFF7043, PaletteStyle.Expressive),
)

private fun apply(context: Context, uiPreferences: UiPreferences, seed: Long, style: PaletteStyle) {
    uiPreferences.colorTheme().set(seed.toInt())
    uiPreferences.customThemeStyle().set(style)
    uiPreferences.appTheme().set(AppTheme.CUSTOM)
    (context as? Activity)?.let { ActivityCompat.recreate(it) }
}

/** 69 mod U5 / U6: ready-made color themes, and theme files to share. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Mod69ThemeTools(uiPreferences: UiPreferences) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        object : ActivityResultContracts.CreateDocument("application/json") {
            override fun createIntent(context: Context, input: String): Intent =
                eu.kanade.tachiyomi.util.system.Mod69Picker.apply(context, super.createIntent(context, input))
        },
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val json = JSONObject()
                .put("seed", uiPreferences.colorTheme().get())
                .put("style", uiPreferences.customThemeStyle().get().name)
                .put("amoled", uiPreferences.themeDarkAmoled().get())
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toString(2).toByteArray()) }
        }.onSuccess { context.toast(KMR.strings.mod69_theme_exported) }
            .onFailure { context.toast(it.message ?: "Error") }
    }
    val importLauncher = rememberLauncherForActivityResult(
        object : ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: Context, input: Array<String>): Intent =
                eu.kanade.tachiyomi.util.system.Mod69Picker.apply(context, super.createIntent(context, input))
        },
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("Cannot read the file")
            val json = JSONObject(text)
            val style = PaletteStyle.entries.firstOrNull { it.name == json.getString("style") }
                ?: PaletteStyle.TonalSpot
            uiPreferences.themeDarkAmoled().set(json.optBoolean("amoled", false))
            apply(context, uiPreferences, json.getInt("seed").toLong() and 0xFFFFFFFFL, style)
        }.onFailure { context.toast(KMR.strings.mod69_theme_import_failed) }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(KMR.strings.mod69_theme_presets),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(KMR.strings.mod69_theme_presets_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                AssistChip(
                    onClick = { apply(context, uiPreferences, preset.seed, preset.style) },
                    label = { Text(preset.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(preset.seed.toInt()), CircleShape),
                        )
                    },
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { exportLauncher.launch("69-komikku-theme.json") }) {
                Text(stringResource(KMR.strings.mod69_theme_export))
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                Text(stringResource(KMR.strings.mod69_theme_import))
            }
        }
    }
}
// KMK <--
