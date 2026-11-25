package com.btcontrol.app.ui.screens

import com.btcontrol.app.R
import com.btcontrol.app.t
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.ScanSuffix
import com.btcontrol.app.ui.components.rememberHaptic
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

private data class ScanEntry(val value: String, val format: String, val time: Long)

/** Barcode / QR scanner that types what it reads on the computer, like a USB barcode reader. */
@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    if (!granted) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(t(R.string.the_code_scanner_needs_the_camera_to_read), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.padding(8.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text(t(R.string.allow_camera)) }
        }
        return
    }
    ScannerContent()
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun ScannerContent() {
    val app = BtControlApp.instance
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val history = remember { mutableStateListOf<ScanEntry>() }
    var pending by remember { mutableStateOf<ScanEntry?>(null) }
    var torch by remember { mutableStateOf(false) }
    val tick = rememberHaptic()

    fun send(value: String) {
        val suffix = when (app.settings.value.scanSuffix) {
            ScanSuffix.NONE -> ""
            ScanSuffix.ENTER -> "\n"
            ScanSuffix.TAB -> "\t"
            ScanSuffix.SPACE -> " "
        }
        val clean = value.filter { it == '\n' || it == '\t' || !it.isISOControl() }
        app.hid.typeText(clean + suffix)
    }

    val onCode by rememberUpdatedState { e: ScanEntry ->
        tick()
        history.add(0, e)
        if (history.size > 50) history.removeAt(history.lastIndex)
        if (app.settings.value.scanAutoSend) send(e.value) else pending = e
    }

    val controller = remember { LifecycleCameraController(context) }
    DisposableEffect(lifecycleOwner) {
        val scanner = BarcodeScanning.getClient()
        val executor = Executors.newSingleThreadExecutor()
        var lastValue = ""
        var lastTime = 0L
        controller.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        controller.setImageAnalysisAnalyzer(executor) { proxy ->
            val media = proxy.image
            if (media == null) {
                proxy.close(); return@setImageAnalysisAnalyzer
            }
            val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
            scanner.process(img)
                .addOnSuccessListener { codes ->
                    val code = codes.firstOrNull { !it.rawValue.isNullOrEmpty() } ?: return@addOnSuccessListener
                    val v = code.rawValue!!
                    val t = SystemClock.elapsedRealtime()
                    // Same code still in view → ignore for a moment (avoids typing it 10 times).
                    if (v == lastValue && t - lastTime < 2500) { lastTime = t; return@addOnSuccessListener }
                    lastValue = v
                    lastTime = t
                    onCode(ScanEntry(v, formatName(code.format), System.currentTimeMillis()))
                }
                .addOnCompleteListener { proxy.close() }
        }
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
            controller.clearImageAnalysisAnalyzer()
            scanner.close()
            executor.shutdown()
        }
    }
    DisposableEffect(torch) {
        controller.enableTorch(torch)
        onDispose { }
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val camera: @Composable (Modifier) -> Unit = { m ->
        Box(m.clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } },
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.align(Alignment.Center).fillMaxWidth(0.7f).fillMaxSize(0.45f)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            )
            IconButton(onClick = { torch = !torch }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(if (torch) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff, t(R.string.torch), tint = Color.White)
            }
        }
    }
    val panel: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.scanAutoSend,
                    onClick = { app.settings.update { it.copy(scanAutoSend = !it.scanAutoSend) } },
                    label = { Text(t(R.string.send_automatically)) },
                )
                SuffixChip(settings.scanSuffix) { s -> app.settings.update { it.copy(scanSuffix = s) } }
            }
            pending?.let { p ->
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.value, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Button(onClick = { send(p.value); pending = null }) { Text(t(R.string.send)) }
                }
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                if (history.isEmpty()) item {
                    Text(
                        t(R.string.point_the_camera_at_a_barcode_or_qr_code_i),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                items(history) { e ->
                    ListItem(
                        headlineContent = { Text(e.value, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${e.format} · ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(e.time))}") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { copy(context, e.value) }) { Icon(Icons.Outlined.ContentCopy, t(R.string.copy_2)) }
                                IconButton(onClick = { send(e.value) }) { Icon(Icons.AutoMirrored.Outlined.Send, t(R.string.send_again)) }
                            }
                        },
                    )
                }
            }
        }
    }

    if (landscape) {
        Row(Modifier.fillMaxSize().padding(12.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            camera(Modifier.weight(1.2f).fillMaxHeight())
            panel(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(12.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            camera(Modifier.fillMaxWidth().weight(1f))
            panel(Modifier.fillMaxWidth().weight(0.9f))
        }
    }
}

@Composable
private fun SuffixChip(value: ScanSuffix, onChange: (ScanSuffix) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(selected = false, onClick = { open = true }, label = { Text(t(R.string.then_suffix, suffixName(value))) })
        DropdownMenu(open, onDismissRequest = { open = false }) {
            for (s in ScanSuffix.entries) DropdownMenuItem(text = { Text(suffixName(s)) }, onClick = { onChange(s); open = false })
        }
    }
}

private fun suffixName(s: ScanSuffix) = when (s) {
    ScanSuffix.NONE -> t(R.string.suffix_nothing)
    ScanSuffix.ENTER -> "Enter"
    ScanSuffix.TAB -> "Tab"
    ScanSuffix.SPACE -> t(R.string.space_2)
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("code", text))
}

private fun formatName(f: Int) = when (f) {
    Barcode.FORMAT_QR_CODE -> t(R.string.qr_code)
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    Barcode.FORMAT_CODE_128 -> "Code 128"
    Barcode.FORMAT_CODE_39 -> "Code 39"
    Barcode.FORMAT_CODE_93 -> "Code 93"
    Barcode.FORMAT_CODABAR -> "Codabar"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_AZTEC -> "Aztec"
    else -> t(R.string.barcode)
}
