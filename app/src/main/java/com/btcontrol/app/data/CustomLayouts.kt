package com.btcontrol.app.data

import com.btcontrol.app.R
import com.btcontrol.app.t
import android.content.Context
import com.btcontrol.app.hid.Consumer
import com.btcontrol.app.hid.HidController
import com.btcontrol.app.hid.Key
import com.btcontrol.app.hid.Mod
import com.btcontrol.app.hid.MouseButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

enum class ActionType { NONE, KEY, TEXT, MEDIA, MOUSE, SYSTEM, MACRO }
enum class ItemKind { BUTTON, TOUCHPAD, SCROLL_STRIP, SPACER }

/** What a custom button does. */
@Serializable
data class ButtonAction(
    val type: ActionType = ActionType.KEY,
    /** KEY: modifier bits. */
    val mods: Int = 0,
    /** KEY: key usage. */
    val key: Int = 0,
    /** TEXT: text to type. */
    val text: String = "",
    /** MEDIA: consumer usage. MOUSE: button bit. SYSTEM: system bit. */
    val usage: Int = 0,
    /** MACRO: steps run in order with [delayMs] between them. */
    val steps: List<ButtonAction> = emptyList(),
    val delayMs: Int = 60,
) {
    fun describe(): String = when (type) {
        ActionType.NONE -> t(R.string.nothing)
        ActionType.KEY -> listOfNotNull(Mod.describe(mods).ifEmpty { null }, keyName(key)).joinToString("+")
        ActionType.TEXT -> t(R.string.action_type_text, text.replace("\n", "⏎"))
        ActionType.MEDIA -> Consumer.NAMES[usage] ?: "Media 0x%X".format(usage)
        ActionType.MOUSE -> MouseButton.NAMES[usage] ?: t(R.string.mouse)
        ActionType.SYSTEM -> when (usage) { 1 -> t(R.string.power); 2 -> t(R.string.sleep); else -> t(R.string.wake) }
        ActionType.MACRO -> t(R.string.action_macro_steps, steps.size)
    }

    companion object {
        fun keyName(usage: Int): String {
            Key.NAMES[usage]?.let { return it }
            return when (usage) {
                in Key.A..Key.Z -> ('A' + (usage - Key.A)).toString()
                in Key.N1..Key.N9 -> ('1' + (usage - Key.N1)).toString()
                Key.N0 -> "0"
                0 -> "?"
                else -> "Key 0x%02X".format(usage)
            }
        }
    }
}

@Serializable
data class LayoutItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val kind: ItemKind = ItemKind.BUTTON,
    val action: ButtonAction = ButtonAction(ActionType.NONE),
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    /** ARGB, 0 = theme default. */
    val color: Long = 0,
)

@Serializable
data class CustomLayout(
    val id: String = UUID.randomUUID().toString(),
    val name: String = t(R.string.new_layout),
    val columns: Int = 4,
    val rowHeightDp: Int = 72,
    val landscape: Boolean = false,
    val items: List<LayoutItem> = emptyList(),
)

/** Runs a [ButtonAction] through the HID controller. */
object ActionRunner {
    /** Called on finger down. Keys, mouse buttons and media keys are held while pressed. */
    fun press(hid: HidController, a: ButtonAction) {
        when (a.type) {
            ActionType.KEY -> {
                modUsages(a.mods).forEach { hid.keyDown(it) }
                if (a.key != 0) hid.keyDown(hid.resolve(a.key))
            }
            ActionType.MEDIA -> hid.consumerDown(a.usage)
            ActionType.MOUSE -> hid.mouseDown(a.usage)
            else -> Unit
        }
    }

    /** Called on finger up. */
    fun release(hid: HidController, a: ButtonAction) {
        when (a.type) {
            ActionType.KEY -> {
                if (a.key != 0) hid.keyUp(hid.resolve(a.key))
                modUsages(a.mods).reversed().forEach { hid.keyUp(it) }
            }
            ActionType.MEDIA -> hid.consumerUp()
            ActionType.MOUSE -> hid.mouseUp(a.usage)
            ActionType.TEXT -> hid.typeText(a.text)
            ActionType.SYSTEM -> hid.systemTap(a.usage)
            ActionType.MACRO -> runMacro(hid, a)
            ActionType.NONE -> Unit
        }
    }

    /** One-shot execution (used for macro steps). */
    fun run(hid: HidController, a: ButtonAction) {
        when (a.type) {
            ActionType.KEY -> hid.shortcut(a.key, a.mods)
            ActionType.MEDIA -> hid.consumerTap(a.usage)
            ActionType.MOUSE -> hid.click(a.usage)
            ActionType.TEXT -> hid.typeText(a.text)
            ActionType.SYSTEM -> hid.systemTap(a.usage)
            ActionType.MACRO -> runMacro(hid, a)
            ActionType.NONE -> Unit
        }
    }

    private fun runMacro(hid: HidController, a: ButtonAction) {
        val steps = a.steps
        Thread {
            for ((i, s) in steps.withIndex()) {
                run(hid, s)
                if (i < steps.lastIndex) Thread.sleep(a.delayMs.toLong().coerceIn(0, 5000))
            }
        }.start()
    }

    fun modUsages(mods: Int): List<Int> = buildList {
        for (bit in 0..7) if (mods and (1 shl bit) != 0) add(Key.LEFT_CTRL + bit)
    }
}

/** Stores custom layouts as JSON in the app's files directory. */
class LayoutRepository(context: Context) {
    private val file = File(context.filesDir, "layouts.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val listSer = ListSerializer(CustomLayout.serializer())

    private val _layouts = MutableStateFlow(load())
    val layouts: StateFlow<List<CustomLayout>> = _layouts.asStateFlow()

    private fun load(): List<CustomLayout> {
        if (!file.exists()) return DefaultLayouts.all().also { persist(it) }
        return try {
            json.decodeFromString(listSer, file.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<CustomLayout>) {
        runCatching { file.writeText(json.encodeToString(listSer, list)) }
    }

    fun get(id: String): CustomLayout? = _layouts.value.firstOrNull { it.id == id }

    fun save(layout: CustomLayout) {
        val list = _layouts.value.toMutableList()
        val i = list.indexOfFirst { it.id == layout.id }
        if (i >= 0) list[i] = layout else list += layout
        _layouts.value = list
        persist(list)
    }

    fun delete(id: String) {
        val list = _layouts.value.filterNot { it.id == id }
        _layouts.value = list
        persist(list)
    }

    fun duplicate(id: String): CustomLayout? {
        val src = get(id) ?: return null
        val copy = src.copy(
            id = UUID.randomUUID().toString(),
            name = src.name + t(R.string.copy),
            items = src.items.map { it.copy(id = UUID.randomUUID().toString()) },
        )
        save(copy)
        return copy
    }

    fun move(id: String, delta: Int) {
        val list = _layouts.value.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        list.add(j, list.removeAt(i))
        _layouts.value = list
        persist(list)
    }

    fun exportJson(id: String): String? = get(id)?.let { json.encodeToString(CustomLayout.serializer(), it) }

    fun exportAllJson(): String = json.encodeToString(listSer, _layouts.value)

    /** Accepts a single layout or a list. Imported layouts get fresh IDs. Returns how many were added. */
    fun importJson(text: String): Int {
        val trimmed = text.trim()
        val imported: List<CustomLayout> = if (trimmed.startsWith("[")) json.decodeFromString(listSer, trimmed)
        else listOf(json.decodeFromString(CustomLayout.serializer(), trimmed))
        imported.forEach { l ->
            save(l.copy(id = UUID.randomUUID().toString(), items = l.items.map { it.copy(id = UUID.randomUUID().toString()) }))
        }
        return imported.size
    }

    fun restoreExamples() {
        DefaultLayouts.all().forEach { save(it.copy(id = UUID.randomUUID().toString())) }
    }
}

/** Example layouts created on first launch. */
object DefaultLayouts {
    private fun key(label: String, key: Int, mods: Int = 0, span: Int = 1, color: Long = 0) =
        LayoutItem(label = label, action = ButtonAction(ActionType.KEY, mods = mods, key = key), colSpan = span, color = color)

    private fun media(label: String, usage: Int, span: Int = 1) =
        LayoutItem(label = label, action = ButtonAction(ActionType.MEDIA, usage = usage), colSpan = span)

    fun all(): List<CustomLayout> = listOf(windows(), video(), browser())

    private fun windows() = CustomLayout(
        name = t(R.string.windows_shortcuts), columns = 4, rowHeightDp = 64,
        items = listOf(
            key("Alt+Tab", Key.TAB, Mod.LALT), key(t(R.string.desktop), Key.D, Mod.LGUI),
            key(t(R.string.explorer), Key.E, Mod.LGUI), key(t(R.string.lock), Key.L, Mod.LGUI, color = 0xFFB3261EL),
            key(t(R.string.copy_2), Key.C, Mod.LCTRL), key(t(R.string.paste), Key.V, Mod.LCTRL),
            key(t(R.string.undo), Key.Z, Mod.LCTRL), key(t(R.string.redo), Key.Y, Mod.LCTRL),
            key(t(R.string.screenshot), Key.S, Mod.LGUI or Mod.LSHIFT), key(t(R.string.task_mgr), Key.ESC, Mod.LCTRL or Mod.LSHIFT),
            key(t(R.string.close), Key.F4, Mod.LALT), key(t(R.string.start), Key.LEFT_GUI),
            LayoutItem(label = t(R.string.kind_touchpad), kind = ItemKind.TOUCHPAD, colSpan = 4, rowSpan = 4),
            LayoutItem(label = t(R.string.left), action = ButtonAction(ActionType.MOUSE, usage = MouseButton.LEFT), colSpan = 2),
            LayoutItem(label = t(R.string.right), action = ButtonAction(ActionType.MOUSE, usage = MouseButton.RIGHT), colSpan = 2),
        ),
    )

    private fun video() = CustomLayout(
        name = t(R.string.video_player), columns = 3, rowHeightDp = 80,
        items = listOf(
            key(t(R.string.fullscreen), Key.F), key(t(R.string.play_pause), Key.SPACE, color = 0xFF2E7D32L), key(t(R.string.mute), Key.M),
            key("−10 s", Key.LEFT), key(t(R.string.volume), Key.UP), key("+10 s", Key.RIGHT),
            media(t(R.string.prev), Consumer.PREV_TRACK), key(t(R.string.volume_2), Key.DOWN), media(t(R.string.next), Consumer.NEXT_TRACK),
            key(t(R.string.exit_fullscreen), Key.ESC, span = 3),
        ),
    )

    private fun browser() = CustomLayout(
        name = t(R.string.web_browser), columns = 4, rowHeightDp = 64,
        items = listOf(
            media(t(R.string.back), Consumer.AC_BACK), media(t(R.string.forward), Consumer.AC_FORWARD),
            key(t(R.string.reload), Key.F5), key(t(R.string.address), Key.L, Mod.LCTRL),
            key(t(R.string.new_tab), Key.T, Mod.LCTRL), key(t(R.string.close_tab), Key.W, Mod.LCTRL),
            key(t(R.string.prev_tab), Key.TAB, Mod.LCTRL or Mod.LSHIFT), key(t(R.string.next_tab), Key.TAB, Mod.LCTRL),
            key(t(R.string.zoom), Key.KP_MINUS, Mod.LCTRL), key(t(R.string.zoom_100), Key.N0, Mod.LCTRL),
            key(t(R.string.zoom_2), Key.KP_PLUS, Mod.LCTRL), key(t(R.string.fullscreen), Key.F11),
            LayoutItem(label = t(R.string.kind_touchpad), kind = ItemKind.TOUCHPAD, colSpan = 3, rowSpan = 4),
            LayoutItem(label = t(R.string.scroll), kind = ItemKind.SCROLL_STRIP, colSpan = 1, rowSpan = 4),
            key(t(R.string.page_up), Key.PAGE_UP, span = 2), key(t(R.string.page_down), Key.PAGE_DOWN, span = 2),
        ),
    )
}
