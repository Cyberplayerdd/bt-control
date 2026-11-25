package com.btcontrol.app.ui.screens

import com.btcontrol.app.R
import com.btcontrol.app.t
import com.btcontrol.app.tq
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.CustomLayout

@Composable
fun ManageLayoutsScreen(onOpen: (String) -> Unit, onEdit: (String) -> Unit) {
    val repo = BtControlApp.instance.layouts
    val layouts by repo.layouts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var toDelete by remember { mutableStateOf<CustomLayout?>(null) }
    var paste by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
            repo.importJson(text)
        }.onSuccess { toast(tq(R.plurals.imported_layouts, it)) }.onFailure { toast(t(R.string.not_a_valid_layout_file)) }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(repo.exportAllJson()) }
        }.onSuccess { toast(t(R.string.saved)) }.onFailure { toast(t(R.string.could_not_save)) }
    }

    toDelete?.let { l ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(t(R.string.delete_named, l.name)) },
            confirmButton = { TextButton(onClick = { repo.delete(l.id); toDelete = null }) { Text(t(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(t(R.string.cancel)) } },
        )
    }
    if (paste) PasteImportDialog(onDismiss = { paste = false }) { text ->
        runCatching { repo.importJson(text) }
            .onSuccess { toast(tq(R.plurals.imported_layouts, it)); paste = false }
            .onFailure { toast(t(R.string.not_a_valid_layout)) }
    }

    LazyColumn(
        Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(onClick = { onEdit("new") }, label = { Text(t(R.string.new_label)) }, leadingIcon = { Icon(Icons.Outlined.Add, null) })
                AssistChip(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, label = { Text(t(R.string.import_label)) }, leadingIcon = { Icon(Icons.Outlined.FileUpload, null) })
                AssistChip(onClick = { exportLauncher.launch("bt-control-layouts.json") }, label = { Text(t(R.string.export_all)) }, leadingIcon = { Icon(Icons.Outlined.FileDownload, null) })
                MoreMenu(onPaste = { paste = true }, onRestore = { repo.restoreExamples(); toast(t(R.string.examples_added)) })
            }
        }
        if (layouts.isEmpty()) item {
            Text(t(R.string.no_layouts_yet_tap_new_to_build_one_from_b), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(layouts, key = { it.id }) { l ->
            Card(onClick = { onOpen(l.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(l.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            tq(R.plurals.layout_items, l.items.size) + " · " + tq(R.plurals.layout_columns, l.columns) + if (l.landscape) t(R.string.layout_summary_landscape) else "",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { repo.move(l.id, -1) }) { Icon(Icons.Outlined.KeyboardArrowUp, t(R.string.move_up)) }
                    IconButton(onClick = { repo.move(l.id, 1) }) { Icon(Icons.Outlined.KeyboardArrowDown, t(R.string.move_down)) }
                    IconButton(onClick = { onEdit(l.id) }) { Icon(Icons.Outlined.Edit, t(R.string.edit)) }
                    LayoutMenu(
                        onDuplicate = { repo.duplicate(l.id) },
                        onShare = {
                            val json = repo.exportJson(l.id) ?: return@LayoutMenu
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, json)
                                .putExtra(Intent.EXTRA_SUBJECT, t(R.string.share_layout_subject, l.name))
                            context.startActivity(Intent.createChooser(send, t(R.string.share_layout)))
                        },
                        onDelete = { toDelete = l },
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutMenu(onDuplicate: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, t(R.string.more)) }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(t(R.string.duplicate)) }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { open = false; onDuplicate() })
            DropdownMenuItem(text = { Text(t(R.string.share_as_text)) }, leadingIcon = { Icon(Icons.Outlined.Share, null) }, onClick = { open = false; onShare() })
            DropdownMenuItem(text = { Text(t(R.string.delete)) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun MoreMenu(onPaste: () -> Unit, onRestore: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, t(R.string.more)) }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(t(R.string.paste_layout_text)) }, onClick = { open = false; onPaste() })
            DropdownMenuItem(text = { Text(t(R.string.add_example_layouts)) }, onClick = { open = false; onRestore() })
        }
    }
}

@Composable
private fun PasteImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(R.string.import_layout)) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text(t(R.string.paste_the_layout_text_json_here)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            )
        },
        confirmButton = { TextButton(onClick = { onImport(text) }) { Text(t(R.string.import_label)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t(R.string.cancel)) } },
    )
}
