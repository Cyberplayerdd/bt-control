package com.btcontrol.app.ui.screens

import com.btcontrol.app.R
import com.btcontrol.app.t
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.data.ActionType
import com.btcontrol.app.data.ButtonAction
import com.btcontrol.app.data.CustomLayout
import com.btcontrol.app.data.ItemKind
import com.btcontrol.app.data.LayoutItem
import com.btcontrol.app.hid.Consumer
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.Mod
import com.btcontrol.app.hid.MouseButton
import com.btcontrol.app.hid.SystemKey
import com.btcontrol.app.ui.components.Dropdown

private val KEY_CHOICES: List<Pair<Int, String>> get() = buildList {
    for (i in 0 until 26) add(Key.A + i to ('A' + i).toString())
    for (i in 1..9) add(Key.N1 + i - 1 to "$i")
    add(Key.N0 to "0")
    Key.NAMES.filterKeys { !Key.isModifier(it) }.forEach { (k, v) -> add(k to v) }
    add(Key.LEFT_GUI to t(R.string.win_alone))
    add(Key.MINUS to t(R.string.us_key_position, "-"))
    add(Key.EQUAL to t(R.string.us_key_position, "="))
    add(Key.LEFT_BRACKET to t(R.string.us_key_position, "["))
    add(Key.RIGHT_BRACKET to t(R.string.us_key_position, "]"))
    add(Key.BACKSLASH to t(R.string.us_key_position, "\\"))
    add(Key.SEMICOLON to t(R.string.us_key_position, ";"))
    add(Key.APOSTROPHE to t(R.string.us_key_position, "'"))
    add(Key.GRAVE to t(R.string.us_key_position, "`"))
    add(Key.COMMA to t(R.string.us_key_position, ","))
    add(Key.PERIOD to t(R.string.us_key_position, "."))
    add(Key.SLASH to t(R.string.us_key_position, "/"))
    add(Key.NON_US_BACKSLASH to t(R.string.iso_extra_key, "<>"))
}

private val PALETTE = listOf(
    0L, 0xFF1E5BD8L, 0xFF2E7D32L, 0xFFC62828L, 0xFFF9A825L, 0xFF6A1B9AL, 0xFF00897BL, 0xFFEF6C00L, 0xFF546E7AL,
)

@Composable
fun LayoutEditorScreen(id: String, onDone: () -> Unit) {
    val repo = BtControlApp.instance.layouts
    val isNew = id == "new"
    var draft by remember(id) { mutableStateOf(repo.get(id) ?: CustomLayout(name = t(R.string.my_layout))) }
    var editing by remember { mutableStateOf<LayoutItem?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun updateItem(item: LayoutItem) {
        val list = draft.items.toMutableList()
        val i = list.indexOfFirst { it.id == item.id }
        if (i >= 0) list[i] = item else list += item
        draft = draft.copy(items = list)
    }

    fun addItem(kind: ItemKind) {
        val item = when (kind) {
            ItemKind.BUTTON -> LayoutItem(label = "", action = ButtonAction(ActionType.KEY, key = Key.ENTER))
            ItemKind.TOUCHPAD -> LayoutItem(label = t(R.string.kind_touchpad), kind = kind, colSpan = draft.columns, rowSpan = 3)
            ItemKind.SCROLL_STRIP -> LayoutItem(label = t(R.string.scroll), kind = kind, rowSpan = 3)
            ItemKind.SPACER -> LayoutItem(kind = kind)
        }
        draft = draft.copy(items = draft.items + item)
        if (kind == ItemKind.BUTTON) editing = item
    }

    editing?.let { item ->
        ItemEditorDialog(
            item = item,
            columns = draft.columns,
            onSave = { updateItem(it); editing = null },
            onDelete = { draft = draft.copy(items = draft.items.filterNot { x -> x.id == item.id }); editing = null },
            onDismiss = { editing = null },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t(R.string.delete_named, draft.name)) },
            confirmButton = { TextButton(onClick = { repo.delete(draft.id); confirmDelete = false; onDone() }) { Text(t(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(t(R.string.cancel)) } },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text(t(R.string.layout_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t(R.string.columns), Modifier.weight(1f))
                Stepper(draft.columns, 1..8) { draft = draft.copy(columns = it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t(R.string.row_height), Modifier.weight(1f))
                Stepper(draft.rowHeightDp, 40..160, step = 8, suffix = " dp") { draft = draft.copy(rowHeightDp = it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t(R.string.landscape), Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = draft.landscape, onCheckedChange = { draft = draft.copy(landscape = it) })
            }
            HorizontalDivider()
            Text(t(R.string.preview_tap_an_item_to_edit_it), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).padding(10.dp)
            ) {
                LayoutGrid(draft, editing = true, selectedId = editing?.id, onSelect = { editing = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AddChip(t(R.string.kind_button)) { addItem(ItemKind.BUTTON) }
                AddChip(t(R.string.kind_touchpad)) { addItem(ItemKind.TOUCHPAD) }
                AddChip(t(R.string.scroll)) { addItem(ItemKind.SCROLL_STRIP) }
                AddChip(t(R.string.space_2)) { addItem(ItemKind.SPACER) }
            }
            HorizontalDivider()
            Text(t(R.string.items_order_placement_order), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            draft.items.forEachIndexed { i, item ->
                Row(
                    Modifier.fillMaxWidth().clickable { editing = item }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.label.ifBlank { if (item.kind == ItemKind.BUTTON) item.action.describe() else item.kind.name.lowercase() },
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${kindName(item.kind)} · ${item.colSpan}×${item.rowSpan}" +
                                if (item.kind == ItemKind.BUTTON) " · ${item.action.describe()}" else "",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(enabled = i > 0, onClick = {
                        val l = draft.items.toMutableList(); l.add(i - 1, l.removeAt(i)); draft = draft.copy(items = l)
                    }) { Icon(Icons.Outlined.KeyboardArrowUp, t(R.string.move_up)) }
                    IconButton(enabled = i < draft.items.lastIndex, onClick = {
                        val l = draft.items.toMutableList(); l.add(i + 1, l.removeAt(i)); draft = draft.copy(items = l)
                    }) { Icon(Icons.Outlined.KeyboardArrowDown, t(R.string.move_down)) }
                    IconButton(onClick = { draft = draft.copy(items = draft.items.filterNot { it.id == item.id }) }) {
                        Icon(Icons.Outlined.Delete, t(R.string.delete))
                    }
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isNew) TextButton(onClick = { confirmDelete = true }) { Text(t(R.string.delete_layout), color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDone) { Text(t(R.string.cancel)) }
                Button(onClick = {
                    repo.save(draft.copy(name = draft.name.ifBlank { t(R.string.layout) }))
                    onDone()
                }) { Text(t(R.string.save)) }
            }
        }
    }
}

@Composable
private fun AddChip(label: String, onClick: () -> Unit) {
    FilterChip(selected = false, onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)) })
}

@Composable
private fun Stepper(value: Int, range: IntRange, step: Int = 1, suffix: String = "", onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange((value - step).coerceIn(range)) }, enabled = value > range.first) { Icon(Icons.Outlined.Remove, t(R.string.less)) }
        Text("$value$suffix", modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { onChange((value + step).coerceIn(range)) }, enabled = value < range.last) { Icon(Icons.Outlined.Add, t(R.string.more)) }
    }
}

@Composable
private fun ItemEditorDialog(
    item: LayoutItem,
    columns: Int,
    onSave: (LayoutItem) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var it by remember(item.id) { mutableStateOf(item) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(R.string.edit_item)) },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Dropdown(
                    it.kind,
                    listOf(ItemKind.BUTTON to t(R.string.kind_button), ItemKind.TOUCHPAD to t(R.string.kind_touchpad), ItemKind.SCROLL_STRIP to t(R.string.kind_scroll_strip), ItemKind.SPACER to t(R.string.kind_space)),
                ) { k -> it = it.copy(kind = k) }
                if (it.kind != ItemKind.SPACER) {
                    OutlinedTextField(
                        value = it.label, onValueChange = { v -> it = it.copy(label = v) },
                        label = { Text(t(R.string.label)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t(R.string.width_columns), Modifier.weight(1f))
                    Stepper(it.colSpan, 1..columns) { v -> it = it.copy(colSpan = v) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t(R.string.height_rows), Modifier.weight(1f))
                    Stepper(it.rowSpan, 1..12) { v -> it = it.copy(rowSpan = v) }
                }
                if (it.kind == ItemKind.BUTTON) {
                    Text(t(R.string.colour), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in PALETTE) {
                            val col = if (c == 0L) MaterialTheme.colorScheme.surfaceContainerHigh else Color(c.toInt())
                            Box(
                                Modifier.size(26.dp).background(col, CircleShape)
                                    .border(if (it.color == c) 3.dp else 1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { it = it.copy(color = c) }
                            )
                        }
                    }
                    HorizontalDivider()
                    Text(t(R.string.action), style = MaterialTheme.typography.labelLarge)
                    ActionEditor(it.action, allowMacro = true) { a -> it = it.copy(action = a) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(it) }) { Text("OK") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text(t(R.string.delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(t(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun ActionEditor(action: ButtonAction, allowMacro: Boolean, onChange: (ButtonAction) -> Unit) {
    val types = buildList {
        add(ActionType.KEY to t(R.string.key_shortcut))
        add(ActionType.TEXT to t(R.string.type_text))
        add(ActionType.MEDIA to t(R.string.media_app_key))
        add(ActionType.MOUSE to t(R.string.mouse_button))
        add(ActionType.SYSTEM to t(R.string.power_sleep))
        if (allowMacro) add(ActionType.MACRO to t(R.string.macro_sequence))
        add(ActionType.NONE to t(R.string.nothing))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Dropdown(action.type, types) { t ->
            onChange(
                when (t) {
                    ActionType.MEDIA -> action.copy(type = t, usage = Consumer.PLAY_PAUSE)
                    ActionType.MOUSE -> action.copy(type = t, usage = MouseButton.LEFT)
                    ActionType.SYSTEM -> action.copy(type = t, usage = SystemKey.SLEEP)
                    else -> action.copy(type = t)
                }
            )
        }
        when (action.type) {
            ActionType.KEY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((bit, name) in listOf(Mod.LCTRL to "Ctrl", Mod.LSHIFT to "Shift", Mod.LALT to "Alt", Mod.RALT to "AltGr", Mod.LGUI to "Win")) {
                        FilterChip(
                            selected = action.mods and bit != 0,
                            onClick = { onChange(action.copy(mods = action.mods xor bit)) },
                            label = { Text(name, fontSize = 11.sp) },
                        )
                    }
                }
                Dropdown(action.key, KEY_CHOICES) { k -> onChange(action.copy(key = k)) }
                Text(
                    t(R.string.letters_follow_your_pc_keyboard_layout_so),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ActionType.TEXT -> OutlinedTextField(
                value = action.text, onValueChange = { onChange(action.copy(text = it)) },
                label = { Text(t(R.string.text_use_a_new_line_for_enter)) }, modifier = Modifier.fillMaxWidth(),
            )
            ActionType.MEDIA -> Dropdown(action.usage, Consumer.NAMES.toList()) { onChange(action.copy(usage = it)) }
            ActionType.MOUSE -> Dropdown(action.usage, MouseButton.NAMES.toList()) { onChange(action.copy(usage = it)) }
            ActionType.SYSTEM -> Dropdown(
                action.usage,
                listOf(SystemKey.SLEEP to t(R.string.sleep), SystemKey.POWER to t(R.string.power_button), SystemKey.WAKE to t(R.string.wake_up)),
            ) { onChange(action.copy(usage = it)) }
            ActionType.MACRO -> MacroEditor(action, onChange)
            ActionType.NONE -> Unit
        }
    }
}

@Composable
private fun MacroEditor(action: ButtonAction, onChange: (ButtonAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        action.steps.forEachIndexed { i, step ->
            Column(
                Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t(R.string.step_n, i + 1), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onChange(action.copy(steps = action.steps.filterIndexed { j, _ -> j != i })) }) {
                        Icon(Icons.Outlined.Delete, t(R.string.remove_step))
                    }
                }
                ActionEditor(step, allowMacro = false) { s ->
                    onChange(action.copy(steps = action.steps.toMutableList().also { it[i] = s }))
                }
            }
        }
        OutlinedButton(onClick = {
            onChange(action.copy(steps = action.steps + ButtonAction(ActionType.KEY, key = Key.ENTER)))
        }) { Icon(Icons.Outlined.Add, null); Text(t(R.string.add_step)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t(R.string.delay_between_steps), Modifier.weight(1f))
            Stepper(action.delayMs, 0..2000, step = 20, suffix = " ms") { onChange(action.copy(delayMs = it)) }
        }
    }
}

private fun kindName(k: ItemKind) = when (k) {
    ItemKind.BUTTON -> t(R.string.kind_button)
    ItemKind.TOUCHPAD -> t(R.string.kind_touchpad)
    ItemKind.SCROLL_STRIP -> t(R.string.kind_scroll_strip)
    ItemKind.SPACER -> t(R.string.kind_space)
}
