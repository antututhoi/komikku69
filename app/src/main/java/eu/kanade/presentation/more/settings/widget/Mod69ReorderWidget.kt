package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// KMK -->
/**
 * 69 mod: a settings row that opens a list where items are moved up / down. [items] are
 * (key, label) pairs in the current order; [onSave] gets the keys in the new order.
 */
@Composable
fun Mod69ReorderPreferenceWidget(
    title: String,
    subtitle: String?,
    items: List<Pair<String, String>>,
    saveText: String,
    cancelText: String,
    onSave: (List<String>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    TextPreferenceWidget(title = title, subtitle = subtitle, onPreferenceClick = { open = true })
    if (!open) return

    val working = remember(items) { mutableStateListOf<Pair<String, String>>().apply { addAll(items) } }
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(title) },
        text = {
            LazyColumn {
                itemsIndexed(working, key = { _, item -> item.first }) { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.second,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                        )
                        IconButton(
                            enabled = index > 0,
                            onClick = { working.add(index - 1, working.removeAt(index)) },
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
                        }
                        IconButton(
                            enabled = index < working.lastIndex,
                            onClick = { working.add(index + 1, working.removeAt(index)) },
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(working.map { it.first })
                    open = false
                },
            ) { Text(saveText) }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text(cancelText) } },
    )
}
// KMK <--
