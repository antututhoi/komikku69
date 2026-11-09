package eu.kanade.presentation.more.settings.widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.materialkolor.PaletteStyle
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.more.settings.screen.appearance.AppCustomThemeColorPickerScreen
import eu.kanade.tachiyomi.util.system.toast
import org.json.JSONArray
import org.json.JSONObject
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource

// KMK -->
private data class Mod69SavedTheme(val name: String, val seed: Int, val secondary: Int, val style: PaletteStyle)

private fun loadThemes(uiPreferences: UiPreferences): List<Mod69SavedTheme> = runCatching {
    val arr = JSONArray(uiPreferences.mod69SavedThemes().get())
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Mod69SavedTheme(
            name = o.getString("name"),
            seed = o.getInt("seed"),
            secondary = o.optInt("secondary", 0),
            style = PaletteStyle.entries.firstOrNull { it.name == o.optString("style") } ?: PaletteStyle.TonalSpot,
        )
    }
}.getOrDefault(emptyList())

private fun saveThemes(uiPreferences: UiPreferences, list: List<Mod69SavedTheme>) {
    val arr = JSONArray()
    list.forEach {
        arr.put(
            JSONObject().put("name", it.name).put("seed", it.seed).put("secondary", it.secondary).put("style", it.style.name),
        )
    }
    uiPreferences.mod69SavedThemes().set(arr.toString())
}

private fun apply(context: Context, uiPreferences: UiPreferences, seed: Long, style: PaletteStyle, secondary: Int = 0) {
    uiPreferences.colorTheme().set(seed.toInt())
    uiPreferences.mod69SecondaryColor().set(secondary)
    uiPreferences.customThemeStyle().set(style)
    uiPreferences.appTheme().set(AppTheme.CUSTOM)
    (context as? Activity)?.let { ActivityCompat.recreate(it) }
}

/** 69 mod U5 / U6: your own named themes (main + second color), and theme files to share. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Mod69ThemeTools(uiPreferences: UiPreferences) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    var themes by remember { mutableStateOf(loadThemes(uiPreferences)) }
    var saving by remember { mutableStateOf(false) }
    val mainColor = remember { uiPreferences.colorTheme().get() }
    val secondColor = remember { uiPreferences.mod69SecondaryColor().get() }

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
                .put("secondary", uiPreferences.mod69SecondaryColor().get())
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
            apply(context, uiPreferences, json.getInt("seed").toLong() and 0xFFFFFFFFL, style, json.optInt("secondary", 0))
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
            themes.forEach { theme ->
                InputChip(
                    selected = false,
                    onClick = { apply(context, uiPreferences, theme.seed.toLong() and 0xFFFFFFFFL, theme.style, theme.secondary) },
                    label = { Text(theme.name) },
                    leadingIcon = {
                        Row {
                            Box(Modifier.size(14.dp).background(Color(theme.seed), CircleShape))
                            if (theme.secondary != 0) {
                                Box(Modifier.size(14.dp).background(Color(theme.secondary), CircleShape))
                            }
                        }
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    themes = themes - theme
                                    saveThemes(uiPreferences, themes)
                                },
                        )
                    },
                )
            }
        }
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { navigator.push(AppCustomThemeColorPickerScreen()) }) {
                Box(Modifier.size(14.dp).background(Color(mainColor), CircleShape))
                Text(stringResource(KMR.strings.mod69_theme_main_color), modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = { navigator.push(AppCustomThemeColorPickerScreen(mod69Secondary = true)) }) {
                if (secondColor != 0) Box(Modifier.size(14.dp).background(Color(secondColor), CircleShape))
                Text(stringResource(KMR.strings.mod69_theme_second_color), modifier = Modifier.padding(start = 6.dp))
            }
            if (secondColor != 0) {
                OutlinedButton(onClick = {
                    uiPreferences.mod69SecondaryColor().set(0)
                    (context as? Activity)?.let { ActivityCompat.recreate(it) }
                }) {
                    Text(stringResource(KMR.strings.mod69_theme_second_auto))
                }
            }
            OutlinedButton(onClick = { saving = true }) {
                Text(stringResource(KMR.strings.mod69_theme_save))
            }
        }
        if (saving) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { saving = false },
                title = { Text(stringResource(KMR.strings.mod69_theme_save)) },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text(stringResource(KMR.strings.mod69_theme_name)) },
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            val t = Mod69SavedTheme(
                                name = name.trim(),
                                seed = uiPreferences.colorTheme().get(),
                                secondary = uiPreferences.mod69SecondaryColor().get(),
                                style = uiPreferences.customThemeStyle().get(),
                            )
                            themes = themes.filterNot { it.name == t.name } + t
                            saveThemes(uiPreferences, themes)
                            uiPreferences.appTheme().set(AppTheme.CUSTOM)
                            saving = false
                        },
                    ) { Text(stringResource(MR.strings.action_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { saving = false }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
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
