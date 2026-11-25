package com.btcontrol.app.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.btcontrol.app.R
import com.btcontrol.app.log.AppLog
import com.btcontrol.app.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** View / share / delete rows for the local diagnostic log (Settings). */
@Composable
fun DiagnosticLogActions() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun share() = scope.launch { shareLog(context) }

    ActionRow(Icons.Outlined.Description, t(R.string.view_log)) { viewing = true }
    ActionRow(Icons.Outlined.Share, t(R.string.share_log)) { share() }
    ActionRow(Icons.Outlined.Delete, t(R.string.delete_logs)) { confirmDelete = true }

    if (viewing) {
        val text by produceState<String?>(null) { value = withContext(Dispatchers.IO) { AppLog.tail() } }
        AlertDialog(
            onDismissRequest = { viewing = false },
            title = { Text(t(R.string.diagnostic_log)) },
            text = {
                SelectionContainer(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text?.ifEmpty { t(R.string.log_empty) } ?: "…",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewing = false; share() }) { Text(t(R.string.share_log)) } },
            dismissButton = { TextButton(onClick = { viewing = false }) { Text(t(R.string.close)) } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t(R.string.delete_logs_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        withContext(Dispatchers.IO) { AppLog.deleteAll() }
                        Toast.makeText(context, t(R.string.logs_deleted), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(t(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(t(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Shares the logs as one text file, only when the user asks (nothing is ever sent automatically). */
private suspend fun shareLog(context: Context) {
    val file = withContext(Dispatchers.IO) { AppLog.exportForSharing(context) }
    if (file == null) {
        Toast.makeText(context, t(R.string.log_empty), Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "BT Control log")
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(send, t(R.string.share_log))) }
}
