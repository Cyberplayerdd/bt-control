package com.btcontrol.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.Led
import com.btcontrol.app.ui.components.UsageKey
import com.btcontrol.app.ui.components.ledOutline

@Composable
fun NumpadScreen() {
    val hid = BtControlApp.instance.hid
    val leds by hid.leds.collectAsStateWithLifecycle()
    val numOn = leds and Led.NUM_LOCK != 0
    val gap = 8.dp

    Column(Modifier.fillMaxSize().padding(12.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(gap)) {
        Text(
            if (numOn) "Num Lock is ON on the computer" else "Num Lock is OFF — digits act as arrows/Home/End",
            style = MaterialTheme.typography.bodySmall,
            color = if (numOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        PadRow(Modifier.weight(0.8f)) {
            P(Key.ESC, "Esc", small = true); P(Key.TAB, "Tab", small = true)
            P(Key.BACKSPACE, "⌫"); P(Key.KP_EQUAL, "=")
        }
        PadRow(Modifier.weight(1f)) {
            P(Key.NUM_LOCK, "Num", small = true, led = numOn); P(Key.KP_SLASH, "/")
            P(Key.KP_ASTERISK, "*"); P(Key.KP_MINUS, "−")
        }
        Row(Modifier.weight(4f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            Column(Modifier.weight(3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                PadRow(Modifier.weight(1f)) { P(Key.KP_7, "7", "Home"); P(Key.KP_8, "8", "↑"); P(Key.KP_9, "9", "PgUp") }
                PadRow(Modifier.weight(1f)) { P(Key.KP_4, "4", "←"); P(Key.KP_5, "5"); P(Key.KP_6, "6", "→") }
                PadRow(Modifier.weight(1f)) { P(Key.KP_1, "1", "End"); P(Key.KP_2, "2", "↓"); P(Key.KP_3, "3", "PgDn") }
                PadRow(Modifier.weight(1f)) { P(Key.KP_0, "0", "Ins", weight = 2f); P(Key.KP_DOT, ".", "Del") }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                V(Key.KP_PLUS, "+")
                V(Key.KP_ENTER, "Enter", small = true)
            }
        }
    }
}

@Composable
private fun PadRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) =
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)

@Composable
private fun RowScope.P(usage: Int, label: String, sub: String? = null, weight: Float = 1f, small: Boolean = false, led: Boolean = false) {
    UsageKey(
        usage, label,
        Modifier.weight(weight).fillMaxHeight().ledOutline(led, MaterialTheme.colorScheme.primary),
        sub = sub, fontSize = if (small) 18.sp else 28.sp,
    )
}

@Composable
private fun ColumnScope.V(usage: Int, label: String, small: Boolean = false) {
    UsageKey(usage, label, Modifier.weight(1f).fillMaxWidth(), fontSize = if (small) 18.sp else 28.sp)
}
