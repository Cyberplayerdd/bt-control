package com.btcontrol.app.ui

import com.btcontrol.app.R
import com.btcontrol.app.t
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.btcontrol.app.BtControlApp
import com.btcontrol.app.bt.HidState
import com.btcontrol.app.bt.HidStatus
import com.btcontrol.app.ui.screens.CustomLayoutScreen
import com.btcontrol.app.ui.screens.DevicesScreen
import com.btcontrol.app.ui.screens.GamepadScreen
import com.btcontrol.app.ui.screens.KeyboardScreen
import com.btcontrol.app.ui.screens.LayoutEditorScreen
import com.btcontrol.app.ui.screens.ManageLayoutsScreen
import com.btcontrol.app.ui.screens.MediaScreen
import com.btcontrol.app.ui.screens.MouseScreen
import com.btcontrol.app.ui.screens.NumpadScreen
import com.btcontrol.app.ui.screens.PresentationScreen
import com.btcontrol.app.ui.screens.ScannerScreen
import com.btcontrol.app.ui.screens.SettingsScreen
import com.btcontrol.app.ui.screens.TabletScreen
import com.btcontrol.app.ui.screens.RemoteScreen
import com.btcontrol.app.ui.components.LockOrientation
import android.content.pm.ActivityInfo
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

enum class Dest(val route: String, private val titleRes: Int, val icon: ImageVector, val immersive: Boolean = false) {
    MOUSE("mouse", R.string.nav_mouse, Icons.Outlined.Mouse),
    MEDIA("media", R.string.nav_media, Icons.Outlined.LiveTv),
    REMOTE("remote", R.string.nav_remote, Icons.Outlined.SettingsRemote),
    KEYBOARD("keyboard", R.string.nav_keyboard, Icons.Outlined.Keyboard),
    NUMPAD("numpad", R.string.nav_numpad, Icons.Outlined.Dialpad),
    PRESENTATION("presentation", R.string.nav_presentation, Icons.Outlined.Slideshow),
    SCANNER("scanner", R.string.nav_scanner, Icons.Outlined.QrCodeScanner),
    GAMEPAD("gamepad", R.string.nav_gamepad, Icons.Outlined.SportsEsports, immersive = true),
    TABLET("tablet", R.string.nav_tablet, Icons.Outlined.Brush, immersive = true),
    LAYOUTS("layouts", R.string.nav_layouts, Icons.Outlined.Build),
    DEVICES("devices", R.string.nav_devices, Icons.Outlined.Devices),
    SETTINGS("settings", R.string.nav_settings, Icons.Outlined.Settings);

    val title: String get() = t(titleRes)

    companion object {
        val CONTROLS = listOf(MOUSE, MEDIA, REMOTE, KEYBOARD, NUMPAD, PRESENTATION, SCANNER, GAMEPAD, TABLET)
        fun of(route: String) = entries.firstOrNull { it.route == route }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(actions: AppActions) {
    val app = BtControlApp.instance
    val settings by app.settings.flow.collectAsStateWithLifecycle()
    val btState by app.bt.state.collectAsStateWithLifecycle()
    val layouts by app.layouts.layouts.collectAsStateWithLifecycle()

    var stackText by rememberSaveable { mutableStateOf(sanitizeStart(settings.lastScreen)) }
    val stack = stackText.split('|')
    val route = stack.last()
    var userFullscreen by rememberSaveable { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigateRoot(r: String) {
        stackText = r
        if (Dest.of(r)?.let { it in Dest.CONTROLS } == true || r.startsWith("layout/")) {
            app.settings.update { it.copy(lastScreen = r) }
        }
        scope.launch { drawerState.close() }
    }
    fun push(r: String) { stackText = "$stackText|$r" }
    fun pop() { if (stack.size > 1) stackText = stack.dropLast(1).joinToString("|") }

    var showWelcome by rememberSaveable { mutableStateOf(!settings.welcomeSeen) }
    if (showWelcome) {
        val done = { showWelcome = false; app.settings.update { it.copy(welcomeSeen = true) } }
        WelcomeDialog(onPair = { done(); navigateRoot(Dest.DEVICES.route) }, onDismiss = done)
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = !drawerState.isOpen && stack.size > 1) { pop() }

    val dest = Dest.of(route)
    val immersive = dest?.immersive == true || userFullscreen
    ImmersiveMode(immersive)

    // Every tab has portrait and landscape layouts, so all follow the screen by default.
    val defaultOrientation = when {
        route.startsWith("layout/") -> if (layouts.firstOrNull { it.id == route.removePrefix("layout/") }?.landscape == true) "landscape" else "auto"
        else -> "auto"
    }
    val rotationLocked = false
    val orientation = if (rotationLocked) "landscape" else settings.orientations[route] ?: defaultOrientation
    LockOrientation(
        when (orientation) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            // Follows the sensor even when the system rotation lock is on (like the gamepad).
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
    )
    fun setOrientation(value: String) {
        app.settings.update { it.copy(orientations = it.orientations + (route to value)) }
    }
    val orientationIcon = when (orientation) {
        "landscape" -> Icons.Outlined.StayCurrentLandscape
        "portrait" -> Icons.Outlined.StayCurrentPortrait
        else -> Icons.Outlined.ScreenRotation
    }
    val orientationName = when (orientation) { "landscape" -> t(R.string.landscape); "portrait" -> t(R.string.portrait); else -> t(R.string.auto_rotate) }

    val title = when {
        dest != null -> dest.title
        route.startsWith("layout/") -> layouts.firstOrNull { it.id == route.removePrefix("layout/") }?.name ?: "Layout"
        route.startsWith("edit/") -> "Edit layout"
        else -> ""
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(
                current = route,
                btState = btState,
                layouts = layouts.map { it.id to it.name },
                onNavigate = ::navigateRoot,
                onNewLayout = {
                    navigateRoot(Dest.LAYOUTS.route)
                    push("edit/new")
                },
                onHelp = {
                    scope.launch { drawerState.close() }
                    showWelcome = true
                },
            )
        },
    ) {
        val content: @Composable (Modifier) -> Unit = { m ->
            Box(m) {
                when {
                    route == Dest.MOUSE.route -> MouseScreen()
                    route == Dest.MEDIA.route -> MediaScreen()
                    route == Dest.REMOTE.route -> RemoteScreen()
                    route == Dest.KEYBOARD.route -> KeyboardScreen()
                    route == Dest.NUMPAD.route -> NumpadScreen()
                    route == Dest.PRESENTATION.route -> PresentationScreen()
                    route == Dest.SCANNER.route -> ScannerScreen()
                    route == Dest.GAMEPAD.route -> GamepadScreen()
                    route == Dest.TABLET.route -> TabletScreen()
                    route == Dest.LAYOUTS.route -> ManageLayoutsScreen(
                        onOpen = { navigateRoot("layout/$it") },
                        onEdit = { push("edit/$it") },
                    )
                    route == Dest.DEVICES.route -> DevicesScreen(actions)
                    route == Dest.SETTINGS.route -> SettingsScreen()
                    route.startsWith("layout/") -> CustomLayoutScreen(
                        id = route.removePrefix("layout/"),
                        onEdit = { push("edit/$it") },
                    )
                    route.startsWith("edit/") -> LayoutEditorScreen(
                        id = route.removePrefix("edit/"),
                        onDone = { pop() },
                    )
                }
            }
        }

        if (immersive) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                content(Modifier.fillMaxSize())
                // A small notch: keeps the whole screen for the controls.
                Notch(
                    btState = btState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    onMenu = { scope.launch { drawerState.open() } },
                    onDevices = { navigateRoot(Dest.DEVICES.route) },
                    orientation = if (rotationLocked) null else orientation,
                    onOrientation = ::setOrientation,
                    onExitFullscreen = if (userFullscreen) ({ userFullscreen = false }) else null,
                )
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            if (stack.size > 1) {
                                IconButton(onClick = { pop() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t(R.string.back)) }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Outlined.Menu, "Menu") }
                            }
                        },
                        actions = {
                            TopBarScreenActions()
                            ConnectionChip(btState) { navigateRoot(Dest.DEVICES.route) }
                            if ((dest in Dest.CONTROLS || route.startsWith("layout/")) && !rotationLocked) {
                                RotationButton(orientation, orientationIcon, ::setOrientation)
                            }
                            if (dest in Dest.CONTROLS || route.startsWith("layout/")) {
                                IconButton(onClick = { userFullscreen = true }) { Icon(Icons.Outlined.Fullscreen, t(R.string.fullscreen)) }
                            }
                        },
                    )
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if (route != Dest.DEVICES.route && route != Dest.SETTINGS.route && !route.startsWith("edit/")) {
                        ConnectionBanner(btState, actions) { navigateRoot(Dest.DEVICES.route) }
                    }
                    content(Modifier.fillMaxSize())
                }
            }
        }
    }
}

private fun sanitizeStart(last: String): String {
    if (last.startsWith("layout/")) return last
    return Dest.of(last)?.takeIf { it in Dest.CONTROLS }?.route ?: Dest.MOUSE.route
}

@Composable
private fun DrawerContent(
    current: String,
    btState: HidState,
    layouts: List<Pair<String, String>>,
    onNavigate: (String) -> Unit,
    onNewLayout: () -> Unit,
    onHelp: () -> Unit,
) {
    ModalDrawerSheet(Modifier.widthIn(max = 330.dp)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Row(Modifier.padding(start = 28.dp, end = 16.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("BT Control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Row(
                Modifier.padding(horizontal = 28.dp).padding(bottom = 8.dp).clickable { onNavigate(Dest.DEVICES.route) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(btState)
                Spacer(Modifier.width(8.dp))
                Text(statusText(btState), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
            DrawerSection(t(R.string.controls))
            for (d in Dest.CONTROLS) DrawerItem(d.title, d.icon, current == d.route) { onNavigate(d.route) }
            HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
            DrawerSection(t(R.string.custom_layouts))
            for ((id, name) in layouts) DrawerItem(name, Icons.Outlined.Widgets, current == "layout/$id") { onNavigate("layout/$id") }
            DrawerItem(t(R.string.new_layout), Icons.Outlined.Add, false, onNewLayout)
            DrawerItem(Dest.LAYOUTS.title, Dest.LAYOUTS.icon, current == Dest.LAYOUTS.route) { onNavigate(Dest.LAYOUTS.route) }
            HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
            DrawerSection(t(R.string.configuration))
            DrawerItem(Dest.DEVICES.title, Dest.DEVICES.icon, current == Dest.DEVICES.route) { onNavigate(Dest.DEVICES.route) }
            DrawerItem(Dest.SETTINGS.title, Dest.SETTINGS.icon, current == Dest.SETTINGS.route) { onNavigate(Dest.SETTINGS.route) }
            DrawerItem(t(R.string.about_help), Icons.AutoMirrored.Outlined.HelpOutline, false, onHelp)
        }
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        icon = { Icon(icon, null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

fun statusText(s: HidState): String = when (s.status) {
    HidStatus.CONNECTED -> t(R.string.status_connected_to, s.deviceName ?: "")
    HidStatus.CONNECTING -> t(R.string.status_connecting_to, s.deviceName ?: t(R.string.computer))
    HidStatus.READY -> t(R.string.not_connected)
    HidStatus.BLUETOOTH_OFF -> t(R.string.bluetooth_is_off)
    HidStatus.NO_PERMISSION -> t(R.string.bluetooth_permission_needed)
    HidStatus.NO_BLUETOOTH -> t(R.string.no_bluetooth_on_this_device)
    HidStatus.UNSUPPORTED -> t(R.string.hid_profile_not_supported)
    HidStatus.REGISTRATION_FAILED -> t(R.string.hid_registration_failed)
    HidStatus.STARTING -> t(R.string.starting)
}

@Composable
fun StatusDot(s: HidState, modifier: Modifier = Modifier) {
    val color = when (s.status) {
        HidStatus.CONNECTED -> Color(0xFF2E7D32)
        HidStatus.CONNECTING, HidStatus.STARTING -> Color(0xFFF9A825)
        HidStatus.READY -> Color(0xFF9E9E9E)
        else -> Color(0xFFC62828)
    }
    Box(modifier.size(10.dp).background(color, CircleShape))
}

@Composable
private fun ConnectionChip(s: HidState, onClick: () -> Unit) {
    // On narrow screens (phones in portrait) only the status dot is shown so the title keeps its room.
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 520
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Row(Modifier.padding(horizontal = if (compact) 14.dp else 10.dp, vertical = if (compact) 14.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(s)
            if (compact) return@Row
            Spacer(Modifier.width(6.dp))
            Text(
                if (s.connected) (s.deviceName ?: t(R.string.connected)) else if (s.status == HidStatus.CONNECTING) t(R.string.connecting) else t(R.string.offline),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp),
            )
        }
    }
}

@Composable
private fun ConnectionBanner(s: HidState, actions: AppActions, openDevices: () -> Unit) {
    if (s.status == HidStatus.CONNECTED) return
    val app = BtControlApp.instance
    val (text, button, action) = when (s.status) {
        HidStatus.NO_PERMISSION -> Triple(t(R.string.bluetooth_permission_is_required), t(R.string.grant), actions.requestPermissions)
        HidStatus.BLUETOOTH_OFF -> Triple(t(R.string.bluetooth_is_off_2), t(R.string.turn_on), actions.enableBluetooth)
        HidStatus.REGISTRATION_FAILED -> Triple(s.message ?: t(R.string.hid_registration_failed_2), t(R.string.retry), { app.bt.reRegister() })
        HidStatus.READY -> {
            val last = app.settings.value.lastDeviceAddress
            val name = app.bt.bondedDevices.value.firstOrNull { it.address == last }?.name
            if (name != null) Triple(t(R.string.not_connected_2), t(R.string.connect_to, name), { app.bt.connect(last!!) })
            else Triple(t(R.string.not_connected_to_a_computer), t(R.string.pair), openDevices)
        }
        HidStatus.CONNECTING -> Triple(t(R.string.status_connecting_to, s.deviceName ?: t(R.string.computer)), t(R.string.devices), openDevices)
        else -> Triple(statusText(s), t(R.string.devices), openDevices)
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = action) { Text(button, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/** Hides the status & navigation bars while [enabled]. */
@Composable
fun ImmersiveMode(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val c = WindowCompat.getInsetsController(window, view)
            if (enabled) {
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                c.hide(WindowInsetsCompat.Type.systemBars())
            } else c.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }
}


/** An option of the current tab, shown in the top bar (or the notch menu in full screen). */
class ScreenAction {
    var label by mutableStateOf("")
    var icon by mutableStateOf<ImageVector?>(null)
    var checked by mutableStateOf<Boolean?>(null)
    var onClick: () -> Unit = {}
}

object ScreenActions {
    val list = mutableStateListOf<ScreenAction>()
}

/** Registers an option of the current tab while the calling composable is shown. */
@Composable
fun ScreenActionEntry(label: String, icon: ImageVector? = null, checked: Boolean? = null, onClick: () -> Unit) {
    // One stable entry per call site: it keeps its place in the menu and only its text changes.
    val entry = remember { ScreenAction() }
    SideEffect {
        entry.label = label
        entry.icon = icon
        entry.checked = checked
        entry.onClick = onClick
    }
    DisposableEffect(entry) {
        ScreenActions.list += entry
        onDispose { ScreenActions.list.remove(entry) }
    }
}

/** Text-only option (shown in the notch menu / overflow menu). */
@Composable
fun NotchEntry(label: String, onClick: () -> Unit) = ScreenActionEntry(label, null, null, onClick)

@Composable
private fun TopBarScreenActions() {
    val actions = ScreenActions.list.toList()
    for (a in actions.filter { it.icon != null }) {
        val on = a.checked == true
        val ic = a.icon ?: continue
        IconButton(onClick = { a.onClick() }) {
            Icon(ic, a.label, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val textOnly = actions.filter { it.icon == null }
    if (textOnly.isNotEmpty()) {
        var open by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, t(R.string.more_options)) }
            DropdownMenu(open, onDismissRequest = { open = false }) {
                for (a in textOnly) DropdownMenuItem(text = { Text(a.label) }, onClick = { a.onClick() })
            }
        }
    }
}

@Composable
private fun Notch(
    btState: HidState,
    modifier: Modifier,
    onMenu: () -> Unit,
    onDevices: () -> Unit,
    orientation: String?,
    onOrientation: (String) -> Unit,
    onExitFullscreen: (() -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))) {
        Row(
            Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                )
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(btState, Modifier.size(8.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(26.dp).height(3.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50)))
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Menu") }, leadingIcon = { Icon(Icons.Outlined.Menu, null) }, onClick = { open = false; onMenu() })
            DropdownMenuItem(
                text = { Text(statusText(btState)) },
                leadingIcon = { StatusDot(btState) },
                onClick = { open = false; onDevices() },
            )
            if (orientation != null) {
                for ((v, label, icon) in ROTATION_CHOICES) {
                    DropdownMenuItem(
                        text = { Text(label) },
                        leadingIcon = { Icon(icon, null) },
                        trailingIcon = { if (v == orientation) Text("✓") },
                        onClick = { onOrientation(v) },
                    )
                }
            }
            for (a in ScreenActions.list.toList()) {
                val chk = a.checked
                val ic = a.icon
                DropdownMenuItem(
                    text = { Text(a.label) },
                    leadingIcon = ic?.let { { Icon(it, null) } },
                    trailingIcon = if (chk != null) ({ Text(if (chk) "✓" else "") }) else null,
                    onClick = { a.onClick() },
                )
            }
            if (onExitFullscreen != null) {
                DropdownMenuItem(text = { Text(t(R.string.exit_fullscreen)) }, leadingIcon = { Icon(Icons.Outlined.FullscreenExit, null) }, onClick = { open = false; onExitFullscreen() })
            }
        }
    }
}

private val ROTATION_CHOICES get() = listOf(
    Triple("auto", t(R.string.auto_rotate), Icons.Outlined.ScreenRotation),
    Triple("landscape", t(R.string.lock_landscape), Icons.Outlined.StayCurrentLandscape),
    Triple("portrait", t(R.string.lock_portrait), Icons.Outlined.StayCurrentPortrait),
)

/** Top-bar button: choose how this tab rotates (remembered per tab). */
@Composable
private fun RotationButton(current: String, icon: ImageVector, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(icon, t(R.string.rotation_of_this_tab)) }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            Text(
                t(R.string.rotation_of_this_tab),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            for ((v, label, ic) in ROTATION_CHOICES) {
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Icon(ic, null) },
                    trailingIcon = { if (v == current) Text("✓") },
                    onClick = { onSelect(v); open = false },
                )
            }
        }
    }
}
