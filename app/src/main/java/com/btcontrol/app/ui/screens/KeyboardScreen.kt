package com.btcontrol.app.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.KeyboardLayout
import com.btcontrol.app.ui.components.KeyboardSizes
import com.btcontrol.app.ui.components.SpecKeyboard

/**
 * PC keyboard in the size of your choice (40 % → full size with numpad). Keys are captioned in
 * the PC layout and show what they will type with Shift / AltGr / Caps Lock / Fn active.
 */
@Composable
fun KeyboardScreen() {
    val app = BtControlApp.instance
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val layout = KeyboardLayout.byId(settings.keyboardLayout)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val size = settings.keyboardSize

    Column(Modifier.fillMaxSize().padding(horizontal = 6.dp).navigationBarsPadding()) {
        // Wraps to a second line on narrow screens / long translations instead of cutting the last chip.
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            for ((id, label) in KeyboardSizes.ALL) {
                FilterChip(
                    selected = size == id,
                    onClick = { app.settings.update { it.copy(keyboardSize = id) } },
                    label = { Text(label) },
                )
            }
            LayoutChip()
        }
        // In portrait the keyboard keeps a keyboard-like height at the bottom.
        val kbModifier = if (landscape) Modifier.weight(1f).fillMaxWidth()
        else Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)
        Column(kbModifier, verticalArrangement = if (landscape) Arrangement.Top else Arrangement.Bottom) {
            val rows = KeyboardSizes.rows(size, layout.iso)
            val m = if (landscape) Modifier.fillMaxSize()
            else Modifier.fillMaxWidth().fillMaxHeight(minOf(1f, rows.size * 0.11f + 0.15f))
            SpecKeyboard(rows, layout, m.padding(bottom = 4.dp), gap = 4.dp, maxFont = if (landscape) 20f else 15f)
        }
    }
}
