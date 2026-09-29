package com.haddadian.universalremote.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.haddadian.universalremote.Command
import com.haddadian.universalremote.TouchpadController
import com.haddadian.universalremote.domain.ConnectionPhase
import com.haddadian.universalremote.domain.RemoteMode

@Composable
fun RemoteControlHeader(state: RemoteUiState, go: (AppDestination) -> Unit) {
    val deviceName = state.selectedDevice?.name ?: "TV"
    val status = if (state.phase is ConnectionPhase.Ready) {
        "● Connected • ${state.method.title} • ${state.health.name}"
    } else {
        "○ Not connected"
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(deviceName, style = MaterialTheme.typography.titleLarge)
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { go(AppDestination.DIAGNOSTICS) }) { Text("⋮") }
    }
}

@Composable
fun RemoteControlMainPanel(state: RemoteUiState, vm: RemoteViewModel) {
    val haptic = LocalHapticFeedback.current
    Box(Modifier.fillMaxWidth()) {
        when (state.mode) {
            RemoteMode.DPAD -> {
                if (state.capabilities.navigation) {
                    Dpad { command ->
                        if (state.settings.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        vm.send(command)
                    }
                } else {
                    UnsupportedControlPanel("کلیدهای ناوبری برای این دستگاه در دسترس نیستند")
                }
            }
            RemoteMode.TOUCHPAD -> GesturePad("Touchpad", vm::send)
            RemoteMode.MOUSE -> GesturePad("Pointer / Mouse fallback", vm::send)
            RemoteMode.KEYBOARD -> KeyboardPad(state.capabilities.keyboard || state.capabilities.clipboard, vm)
        }
    }
}

@Composable
fun RemoteControlBottomControls(state: RemoteUiState, vm: RemoteViewModel, go: (AppDestination) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val actionHaptic: () -> Unit = {
        if (state.settings.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    if (state.settings.oneHandMode) {
        OneHandControls(state, vm, go, actionHaptic)
    } else {
        StandardBottomControls(state, vm, go, actionHaptic)
    }
}

@Composable
private fun OneHandControls(state: RemoteUiState, vm: RemoteViewModel, go: (AppDestination) -> Unit, haptic: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        if (state.capabilities.power) RemoteAction("Power") { haptic(); vm.send(Command.Power) }
        Row(horizontalArrangement = Arrangement.End) {
            if (state.capabilities.volume) RemoteAction("Vol+") { haptic(); vm.send(Command.VolumeUp) }
            if (state.capabilities.mute) RemoteAction("Mute") { haptic(); vm.send(Command.Mute) }
            if (state.capabilities.volume) RemoteAction("Vol−") { haptic(); vm.send(Command.VolumeDown) }
        }
        Row(horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { haptic(); vm.send(Command.Back) }) { Text("Back") }
            TextButton(onClick = { haptic(); vm.send(Command.Home) }) { Text("Home") }
            if (state.capabilities.voice) TextButton(onClick = { go(AppDestination.VOICE) }) { Text("🎙 AI") }
        }
    }
}

@Composable
private fun StandardBottomControls(state: RemoteUiState, vm: RemoteViewModel, go: (AppDestination) -> Unit, haptic: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        if (state.capabilities.power) {
            RemoteAction("Power") {
                if (state.settings.hapticsEnabled) {
                    haptic()
                }
                vm.send(Command.Power)
            }
        }
        if (state.capabilities.volume) RemoteAction("Vol−") { haptic(); vm.send(Command.VolumeDown) }
        if (state.capabilities.mute) RemoteAction("Mute") { haptic(); vm.send(Command.Mute) }
        if (state.capabilities.volume) RemoteAction("Vol+") { haptic(); vm.send(Command.VolumeUp) }
        if (state.capabilities.playback) RemoteAction("▶/Ⅱ") { haptic(); vm.send(Command.PlayPause) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        if (state.capabilities.navigation) {
            TextButton(onClick = { haptic(); vm.send(Command.Back) }) { Text("Back") }
            TextButton(onClick = { haptic(); vm.send(Command.Home) }) { Text("Home") }
        }
        if (state.capabilities.voice) TextButton(onClick = { go(AppDestination.VOICE) }) { Text("🎙 AI") }
    }
    if (state.capabilities.playback) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { haptic(); vm.send(Command.Previous) }) { Text("|◀") }
            TextButton(onClick = { haptic(); vm.send(Command.Rewind) }) { Text("◀◀") }
            TextButton(onClick = { haptic(); vm.send(Command.Stop) }) { Text("■") }
            TextButton(onClick = { haptic(); vm.send(Command.FastForward) }) { Text("▶▶") }
            TextButton(onClick = { haptic(); vm.send(Command.Next) }) { Text("▶|") }
        }
    }
    if (state.capabilities.numberPad) NumberPad(vm)
    if (state.capabilities.input) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = { vm.send(Command.Input) }) { Text("Input / Source") }
            Spacer(Modifier.width(8.dp))
            if (state.capabilities.navigation) OutlinedButton(onClick = { vm.send(Command.Guide) }) { Text("Guide") }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        OutlinedButton(onClick = { go(AppDestination.MACROS) }) { Text("Macros") }
        Spacer(Modifier.width(8.dp))
        if (state.capabilities.appLaunch) OutlinedButton(onClick = { go(AppDestination.APPS) }) { Text("Apps") }
    }
}

@Composable
fun MultiTvSwitcher(state: RemoteUiState, vm: RemoteViewModel) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("TV: ${state.selectedDevice?.name ?: "انتخاب دستگاه"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.devices.forEach { device ->
                DropdownMenuItem(text = { Text(device.name) }, onClick = {
                    vm.select(device)
                    vm.beginConnection()
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun ModeSwitcher(state: RemoteUiState, vm: RemoteViewModel) {
    val supportedModes = buildList {
        if (state.capabilities.navigation) add(RemoteMode.DPAD)
        if (state.capabilities.pointer) {
            add(RemoteMode.TOUCHPAD)
            add(RemoteMode.MOUSE)
        }
        if (state.capabilities.keyboard || state.capabilities.clipboard) add(RemoteMode.KEYBOARD)
    }
    if (supportedModes.isEmpty()) {
        UnsupportedControlPanel("هیچ کنترل اجرایی برای این دستگاه در دسترس نیست")
    } else {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            supportedModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { vm.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, supportedModes.size),
                    label = { Text(mode.name) }
                )
            }
        }
    }
}

@Composable
private fun RemoteAction(label: String, click: () -> Unit) {
    FilledTonalButton(onClick = click, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label) }
}

@Composable
private fun UnsupportedControlPanel(message: String) {
    ElevatedCard(Modifier.fillMaxSize().padding(8.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Dpad(onCommand: (Command) -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        DpadButton("▲") { onCommand(Command.Up) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DpadButton("◀") { onCommand(Command.Left) }
            Spacer(Modifier.width(10.dp))
            DpadButton("OK") { onCommand(Command.Ok) }
            Spacer(Modifier.width(10.dp))
            DpadButton("▶") { onCommand(Command.Right) }
        }
        DpadButton("▼") { onCommand(Command.Down) }
    }
}

@Composable
private fun DpadButton(label: String, click: () -> Unit) {
    OutlinedButton(onClick = click, modifier = Modifier.size(if (label == "OK") 72.dp else 62.dp)) { Text(label) }
}

@Composable
private fun GesturePad(title: String, onCommand: (Command) -> Unit) {
    val controller = remember {
        TouchpadController(
            onKey = { key ->
                onCommand(when (key) {
                    TouchpadController.GestureKey.UP -> Command.Up
                    TouchpadController.GestureKey.DOWN -> Command.Down
                    TouchpadController.GestureKey.LEFT -> Command.Left
                    TouchpadController.GestureKey.RIGHT -> Command.Right
                })
            },
            onSelect = { onCommand(Command.Ok) }
        )
    }
    ElevatedCard(
        Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { controller.tap() }) }
            .pointerInput(Unit) { detectDragGestures { _, drag -> controller.swipe(drag.x, drag.y) } }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("$title\nTap • Drag • Swipe", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FuturePointerPad() {
    ElevatedCard(Modifier.fillMaxSize().padding(8.dp)) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Mouse / Pointer", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("اتصال حرکت واقعی Pointer به Adapter — به‌زودی", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun KeyboardPad(supported: Boolean, vm: RemoteViewModel) {
    var text by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        OutlinedTextField(text, { text = it }, label = { Text("متن برای تلویزیون") }, enabled = supported, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.send(Command.SendText(text)) }, enabled = supported && text.isNotBlank()) { Text("ارسال") }
            OutlinedButton(onClick = { vm.send(Command.SendText("")); text = "" }, enabled = supported && text.isNotEmpty()) { Text("پاک‌کردن") }
        }
        Spacer(Modifier.height(10.dp))
        Text(if (supported) "ارسال متن از مسیر اجرایی دستگاه فعال است" else "این دستگاه Keyboard/Clipboard را گزارش نکرده است", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NumberPad(vm: RemoteViewModel) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        (1..9).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { number ->
                    OutlinedButton(onClick = { vm.send(Command.Number(number)) }) { Text(number.toString()) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = { vm.send(Command.Number(0)) }) { Text("0") }
        }
    }
}
