package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.Mod69CustomNavigation
import tachiyomi.core.common.preference.Preference
import tachiyomi.presentation.core.i18n.stringResource

// KMK -->
/** 69 mod R16: a 3 x 3 picture of the screen. Tap a square to change what tapping there does. */
@Composable
fun Mod69TapGridWidget(
    title: String,
    subtitle: String,
    preference: Preference<String>,
    resetText: String,
) {
    var grid by remember { mutableStateOf(Mod69CustomNavigation.normalize(preference.get())) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .width(180.dp)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            (0 until 3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0 until 3).forEach { column ->
                        val index = row * 3 + column
                        val letter = grid[index]
                        val type = Mod69CustomNavigation.typeOf(letter)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.66f)
                                .background(type?.let { Color(it.color) } ?: Color.Gray)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .clickable {
                                    val letters = Mod69CustomNavigation.LETTERS
                                    val next = letters[(letters.indexOf(letter) + 1) % letters.size]
                                    grid = grid.substring(0, index) + next + grid.substring(index + 1)
                                    preference.set(grid)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = type?.let { stringResource(it.nameRes) } ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = resetText,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    grid = Mod69CustomNavigation.DEFAULT
                    preference.set(grid)
                }
                .padding(top = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
// KMK <--
