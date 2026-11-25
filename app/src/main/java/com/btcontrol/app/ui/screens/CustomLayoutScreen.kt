package com.btcontrol.app.ui.screens

import com.btcontrol.app.R
import com.btcontrol.app.t
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.ActionRunner
import com.btcontrol.app.data.ActionType
import com.btcontrol.app.data.CustomLayout
import com.btcontrol.app.data.ItemKind
import com.btcontrol.app.data.LayoutItem
import com.btcontrol.app.ui.components.CenteredHint
import com.btcontrol.app.ui.components.HoldButton
import com.btcontrol.app.ui.components.ScrollStrip
import com.btcontrol.app.ui.components.Touchpad

@Composable
fun CustomLayoutScreen(id: String, onEdit: (String) -> Unit) {
    val app = BtControlApp.instance
    val layouts by app.layouts.layouts.collectAsStateWithLifecycle()
    val layout = layouts.firstOrNull { it.id == id }
    if (layout == null) {
        CenteredHint(t(R.string.this_layout_no_longer_exists), Modifier.fillMaxSize().padding(32.dp))
        return
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).navigationBarsPadding()) {
            LayoutGrid(layout, editing = false)
            Box(Modifier.height(80.dp))
        }
        FloatingActionButton(
            onClick = { onEdit(layout.id) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp).size(48.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ) { Icon(Icons.Outlined.Edit, t(R.string.edit_layout)) }
    }
}

private data class Placed(val item: LayoutItem, val col: Int, val row: Int, val cs: Int, val rs: Int)

/** Auto-flow placement: each item goes into the first free spot that fits its span. */
private fun pack(items: List<LayoutItem>, columns: Int): Pair<List<Placed>, Int> {
    val cols = columns.coerceIn(1, 12)
    val occ = ArrayList<BooleanArray>()
    fun ensure(r: Int) { while (occ.size <= r) occ.add(BooleanArray(cols)) }
    fun fits(r: Int, c: Int, cs: Int, rs: Int): Boolean {
        for (rr in r until r + rs) { ensure(rr); for (cc in c until c + cs) if (occ[rr][cc]) return false }
        return true
    }
    val out = ArrayList<Placed>()
    for (it in items) {
        val cs = it.colSpan.coerceIn(1, cols)
        val rs = it.rowSpan.coerceIn(1, 20)
        var r = 0
        var placed = false
        while (!placed) {
            for (c in 0..cols - cs) {
                if (fits(r, c, cs, rs)) {
                    for (rr in r until r + rs) for (cc in c until c + cs) occ[rr][cc] = true
                    out += Placed(it, c, r, cs, rs)
                    placed = true
                    break
                }
            }
            r++
        }
    }
    val rows = out.maxOfOrNull { it.row + it.rs } ?: 0
    return out to rows
}

/**
 * Renders a custom layout. In [editing] mode items are not live: tapping selects them.
 */
@Composable
fun LayoutGrid(
    layout: CustomLayout,
    editing: Boolean,
    selectedId: String? = null,
    onSelect: (LayoutItem) -> Unit = {},
) {
    val gap = 8.dp
    val (placed, rows) = pack(layout.items, layout.columns)
    val rowH = layout.rowHeightDp.coerceIn(32, 200).dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cols = layout.columns.coerceIn(1, 12)
        val cellW: Dp = (maxWidth - gap * (cols - 1)) / cols
        val totalH = if (rows == 0) 80.dp else rowH * rows + gap * (rows - 1)
        Box(Modifier.fillMaxWidth().height(totalH)) {
            if (placed.isEmpty()) {
                Text(
                    t(R.string.empty_layout_add_buttons_in_the_editor),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (p in placed) {
                val x = (cellW + gap) * p.col
                val y = (rowH + gap) * p.row
                val w = cellW * p.cs + gap * (p.cs - 1)
                val h = rowH * p.rs + gap * (p.rs - 1)
                val m = Modifier.offset(x, y).size(w, h)
                if (editing) EditPreviewItem(p.item, p.item.id == selectedId, m) { onSelect(p.item) }
                else LiveItem(p.item, m)
            }
        }
    }
}

@Composable
private fun LiveItem(item: LayoutItem, modifier: Modifier) {
    val hid = BtControlApp.instance.hid
    when (item.kind) {
        ItemKind.TOUCHPAD -> Touchpad(modifier, hint = item.label.ifBlank { null })
        ItemKind.SCROLL_STRIP -> Box(modifier, contentAlignment = Alignment.Center) { ScrollStrip(Modifier.fillMaxSize()) }
        ItemKind.SPACER -> Box(modifier)
        ItemKind.BUTTON -> {
            val color = if (item.color != 0L) Color(item.color.toInt()) else null
            HoldButton(
                modifier,
                onPress = { ActionRunner.press(hid, item.action) },
                onRelease = { ActionRunner.release(hid, item.action) },
                containerColor = color?.copy(alpha = 0.25f) ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
            ) { ItemLabel(item, color) }
        }
    }
}

@Composable
private fun ItemLabel(item: LayoutItem, color: Color?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Text(
            item.label.ifBlank { item.action.describe() },
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
        )
        if (item.label.isNotBlank() && item.action.type != ActionType.NONE) {
            Text(
                item.action.describe(),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EditPreviewItem(item: LayoutItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (item.color != 0L) Color(item.color.toInt()) else null
    val bg = when (item.kind) {
        ItemKind.TOUCHPAD -> MaterialTheme.colorScheme.surfaceContainerHighest
        ItemKind.SCROLL_STRIP -> MaterialTheme.colorScheme.surfaceContainerHighest
        ItemKind.SPACER -> Color.Transparent
        ItemKind.BUTTON -> color?.copy(alpha = 0.25f) ?: MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (item.kind) {
            ItemKind.BUTTON -> ItemLabel(item, color)
            ItemKind.TOUCHPAD -> Text(t(R.string.kind_touchpad), color = MaterialTheme.colorScheme.onSurfaceVariant)
            ItemKind.SCROLL_STRIP -> Text(t(R.string.scroll), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ItemKind.SPACER -> Text(t(R.string.space), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
