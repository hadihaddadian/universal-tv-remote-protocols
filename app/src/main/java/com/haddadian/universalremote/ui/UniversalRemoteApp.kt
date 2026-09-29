package com.haddadian.universalremote.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haddadian.universalremote.Command
import com.haddadian.universalremote.TvDevice
import com.haddadian.universalremote.domain.ConnectionMethod
import com.haddadian.universalremote.domain.ConnectionPhase
import com.haddadian.universalremote.domain.RemoteMode

enum class AppDestination { HOME, DEVICES, REMOTE, MORE, DIAGNOSTICS, SETTINGS, APPS, VOICE, MACROS }

@Composable
fun UniversalRemoteApp(vm: RemoteViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    MaterialTheme(colorScheme = if (state.settings.darkMode || destination == AppDestination.REMOTE) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                when (val phase = state.phase) {
                    is ConnectionPhase.Failed -> ConnectionErrorScreen(phase.message, { vm.discover() })
                    is ConnectionPhase.Pairing -> PairingScreen(state, vm)
                    is ConnectionPhase.Discovering, is ConnectionPhase.Connecting -> ConnectionProgressScreen(phase)
                    else -> MainShell(state, vm, destination, { destination = it })
                }
                state.lastError?.let { ErrorSnackbar(it) { vm.clearError() } }
            }
        }
    }
}

@Composable private fun MainShell(state: RemoteUiState, vm: RemoteViewModel, destination: AppDestination, go: (AppDestination)->Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(AppDestination.HOME to "خانه", AppDestination.DEVICES to "دستگاه‌ها", AppDestination.REMOTE to "کنترل", AppDestination.MORE to "بیشتر").forEach { (d, label) ->
                    NavigationBarItem(selected = destination == d, onClick = { go(d) }, icon = { Text(if (d == AppDestination.REMOTE) "●" else "○") }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val contentModifier = if (state.settings.oneHandMode) {
                Modifier.fillMaxWidth().widthIn(max = 440.dp).align(Alignment.CenterEnd)
            } else {
                Modifier.fillMaxSize()
            }
            Box(contentModifier) {
                AnimatedContent(destination, label = "navigation") { screen ->
                    when (screen) {
                    AppDestination.HOME -> HomeScreen(state, vm, go)
                    AppDestination.DEVICES -> DevicesScreen(state, vm, go)
                    AppDestination.REMOTE -> if (state.selectedDevice == null) HomeScreen(state, vm, go) else RemoteControlScreen(state, vm, go)
                    AppDestination.MORE -> MoreScreen(go)
                    AppDestination.DIAGNOSTICS -> DiagnosticsScreen(state)
                    AppDestination.SETTINGS -> SettingsScreen(state, vm)
                    AppDestination.APPS -> AppsScreen(state, vm)
                    AppDestination.VOICE -> VoiceScreen(state, vm)
                    AppDestination.MACROS -> MacrosScreen(state, vm)
                    }
                }
            }
        }
    }
}

@Composable private fun ScreenTitle(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(20.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
        trailing?.invoke()
    }
}

@Composable private fun HomeScreen(state: RemoteUiState, vm: RemoteViewModel, go: (AppDestination)->Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenTitle("Universal Remote", "کنترل هوشمند تلویزیون‌ها", trailing = { TextButton({ go(AppDestination.SETTINGS) }) { Text("تنظیمات") } }) }
        item {
            val d = state.selectedDevice
            ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 20.dp).animateContentSize()) {
                Column(Modifier.padding(20.dp)) {
                    Text(d?.name ?: "تلویزیونی انتخاب نشده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (d != null && state.phase is ConnectionPhase.Ready) "● متصل" else "○ آماده اتصال")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { if (d != null) go(AppDestination.REMOTE) else vm.discover() }, Modifier.fillMaxWidth()) { Text(if (d != null) "باز کردن کنترل" else "جستجوی تلویزیون") }
                }
            }
        }
        item { SectionLabel("دسترسی سریع") }
        item { QuickControls(state, vm, go) }
        item { SectionLabel("تلویزیون‌های پیدا شده") }
        items(state.devices.sortedWith(compareByDescending<TvDevice> { state.favoriteDevices.contains(it.id) }.thenBy { it.name }).take(5), key = { it.id }) { d -> DeviceCard(d, state.selectedDevice?.id == d.id, state.favoriteDevices.contains(d.id), { vm.toggleFavoriteDevice(d.id) }) { vm.select(d); go(AppDestination.REMOTE) } }
        item { TextButton({ vm.discover() }, Modifier.padding(20.dp)) { Text("+ افزودن / جستجوی تلویزیون") } }
    }
}

@Composable private fun QuickControls(state: RemoteUiState, vm: RemoteViewModel, go: (AppDestination)->Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.capabilities.navigation) AssistChip(onClick = { vm.send(Command.Home) }, label = { Text("Home") })
            if (state.capabilities.mute) AssistChip(onClick = { vm.send(Command.Mute) }, label = { Text("Mute") })
            if (state.capabilities.appLaunch) AssistChip(onClick = { go(AppDestination.APPS) }, label = { Text("Apps") })
            if (state.capabilities.voice) AssistChip(onClick = { go(AppDestination.VOICE) }, label = { Text("AI Voice") })
        }
        Text(if (state.favoriteApps.isEmpty()) "Favorite App: هنوز موردی ثبت نشده" else "Favorite Apps: ${state.favoriteApps.size} مورد", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable private fun DevicesScreen(state: RemoteUiState, vm: RemoteViewModel, go: (AppDestination)->Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenTitle("دستگاه‌ها", "مدیریت تلویزیون‌های موجود") }
        item { Button({ vm.discover() }, Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { Text("جستجوی مجدد") } }
        items(state.devices.sortedWith(compareByDescending<TvDevice> { state.favoriteDevices.contains(it.id) }.thenBy { it.name }), key = { it.id }) { d -> DeviceCard(d, state.selectedDevice?.id == d.id, state.favoriteDevices.contains(d.id), { vm.toggleFavoriteDevice(d.id) }) { vm.select(d); vm.beginConnection(); go(AppDestination.REMOTE) } }
        if (state.devices.isEmpty()) item { EmptyState("هنوز تلویزیونی پیدا نشده", "جستجو را آغاز کنید") }
    }
}

@Composable private fun DeviceCard(device: TvDevice, selected: Boolean, favorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("TV", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 14.dp))
            Column(Modifier.weight(1f)) { Text(device.name, fontWeight = FontWeight.Bold); Text(listOfNotNull(device.manufacturer, device.protocol).joinToString(" • "), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Text(if (selected) "Selected" else "Connect", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onFavorite) { Text(if (favorite) "★" else "☆") }
        }
    }
}

@Composable
private fun RemoteControlScreen(state: RemoteUiState, vm: RemoteViewModel, go: (AppDestination)->Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        RemoteControlHeader(state, go)
        if (state.devices.size > 1) MultiTvSwitcher(state, vm)
        ModeSwitcher(state, vm)
        Spacer(Modifier.height(10.dp))
        RemoteControlMainPanel(state, vm)
        RemoteControlBottomControls(state, vm, go)
    }
}

@Composable private fun MacrosScreen(state: RemoteUiState, vm: RemoteViewModel) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenTitle("Macros", "فرمان‌های چندمرحله‌ای محلی") }
        items(state.macros, key = { it.id }) { macro ->
            ListItem(
                headlineContent = { Text(macro.name) },
                supportingContent = { Text("${macro.steps.size} step") },
                trailingContent = { Button({ vm.runMacro(macro.id) }, enabled = state.phase is ConnectionPhase.Ready) { Text("Run") } }
            )
        }
    }
}

@Composable private fun VoiceScreen(state: RemoteUiState, vm: RemoteViewModel) {
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) vm.startVoice()
    }
    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val active = state.voiceActive
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("TV Voice", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Text(if (active) "در حال ارسال صدا به تلویزیون…" else "برای شروع فرمان صوتی لمس کنید")
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (active) {
                    vm.stopVoice()
                } else if (hasPermission) {
                    vm.startVoice()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.size(150.dp),
            shape = RectangleShape,
            enabled = state.capabilities.voice
        ) { Text(if (active) "STOP" else "🎙") }
        Spacer(Modifier.height(16.dp))
        Text(
            when {
                !state.capabilities.voice -> "این تلویزیون قابلیت Voice را گزارش نکرده است."
                permissionDenied -> "دسترسی میکروفون رد شد؛ برای استفاده از Voice باید آن را فعال کنید."
                active -> "صدای میکروفون مستقیماً از مسیر Voice پروتکل تلویزیون ارسال می‌شود."
                else -> "Voice محلی تلویزیون؛ بدون ارسال فرمان‌های خارج از کنترل تلویزیون."
            },
            style = MaterialTheme.typography.bodySmall
        )
        state.lastError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun AppsScreen(state: RemoteUiState, vm: RemoteViewModel) {
    var appLink by rememberSaveable { mutableStateOf("") }
    val common = listOf(
        "YouTube" to "https://www.youtube.com/tv",
        "Netflix" to "https://www.netflix.com/",
        "Prime Video" to "https://www.primevideo.com/",
        "Settings" to "https://www.android.com/"
    )
    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenTitle("Apps", if (state.capabilities.appLaunch) "اجرای App Link از مسیر Android TV" else "این دستگاه App Launch را تأیید نکرده است") }
        if (state.capabilities.appLaunch) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    OutlinedTextField(appLink, { appLink = it }, label = { Text("App Link / URI") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button({ vm.send(Command.LaunchApp(appLink.trim())) }, enabled = appLink.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("اجرای URI") }
                    Spacer(Modifier.height(12.dp))
                    Text("لینک‌های زیر نمونه‌اند؛ اجرای واقعی به پشتیبانی تلویزیون و App Link مقصد وابسته است.", style = MaterialTheme.typography.bodySmall)
                }
            }
            items(common) { (name, link) ->
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(link, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { Text(if (state.favoriteApps.contains(link)) "★" else "☆") },
                    trailingContent = {
                        Row {
                            TextButton({ vm.toggleFavoriteApp(link) }) { Text(if (state.favoriteApps.contains(link)) "حذف نشان" else "نشان‌گذاری") }
                            Button({ vm.send(Command.LaunchApp(link)) }) { Text("اجرا") }
                        }
                    }
                )
            }
        } else {
            item { EmptyState("App Launch در دسترس نیست", "تا زمانی که Capability توسط دستگاه تأیید نشود، دکمه اجرایی نمایش داده نمی‌شود.") }
        }
    }
}

@Composable private fun MoreScreen(go: (AppDestination)->Unit) {
    Column(Modifier.fillMaxSize()) { ScreenTitle("بیشتر")
        listOf("Macros" to AppDestination.MACROS, "Diagnostics" to AppDestination.DIAGNOSTICS, "Settings" to AppDestination.SETTINGS).forEach { (t,d) -> ListItem(headlineContent={Text(t)}, supportingContent={Text(if(d==AppDestination.DIAGNOSTICS) "اتصال، پروتکل و قابلیت‌ها" else "ظاهر، صدا و امنیت")}, modifier=Modifier.fillMaxWidth().clickable { go(d) }) }
    }
}

@Composable private fun DiagnosticsScreen(state: RemoteUiState) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val report = remember(state.phase, state.selectedDevice, state.lastLatencyMs, state.capabilities) {
        buildString {
            appendLine("Universal TV Remote diagnostics")
            appendLine("Device: ${state.selectedDevice?.name.orEmpty()}")
            appendLine("Manufacturer: ${state.selectedDevice?.manufacturer.orEmpty()}")
            appendLine("Protocol: ${state.selectedDevice?.protocol.orEmpty()}")
            appendLine("Address: redacted")
            appendLine("Phase: ${state.phase}")
            appendLine("Health: ${state.health}")
            appendLine("LatencyMs: ${state.lastLatencyMs ?: -1}")
            appendLine("Capabilities: navigation=${state.capabilities.navigation}, playback=${state.capabilities.playback}, keyboard=${state.capabilities.keyboard}, voice=${state.capabilities.voice}, apps=${state.capabilities.appLaunch}, input=${state.capabilities.input}, numberPad=${state.capabilities.numberPad}")
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenTitle("Diagnostics", "اطلاعات فنی بدون credential") }
        item { DiagnosticCard("Connection", state.phase.toString()) }
        item { DiagnosticCard("Protocol", state.selectedDevice?.protocol ?: "—") }
        item { DiagnosticCard("Health", state.health.name) }
        item { DiagnosticCard("Last command latency", state.lastLatencyMs?.let { "$it ms" } ?: "—") }
        item { Button({ clipboard.setText(androidx.compose.ui.text.AnnotatedString(report)) }, Modifier.padding(20.dp).fillMaxWidth()) { Text("Copy safe diagnostic report") } }
    }
}
@Composable private fun DiagnosticCard(k:String,v:String){ ElevatedCard(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=6.dp)){ Column(Modifier.padding(14.dp)){Text(k,fontWeight=FontWeight.Bold);Text(v,style=MaterialTheme.typography.bodySmall)} } }

@Composable private fun SettingsScreen(state: RemoteUiState, vm: RemoteViewModel) {
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Settings")
        SettingSwitch("Dark mode", state.settings.darkMode) { value -> vm.updateSettings { it.copy(darkMode = value) } }
        SettingSwitch("One-hand layout", state.settings.oneHandMode) { value -> vm.updateSettings { it.copy(oneHandMode = value) } }
        SettingSwitch("Haptic feedback", state.settings.hapticsEnabled) { value -> vm.updateSettings { it.copy(hapticsEnabled = value) } }
        ListItem(headlineContent={Text("Voice & AI")}, supportingContent={Text("فقط فرمان‌های کنترل؛ تنظیم موتور — به‌زودی")})
        ListItem(headlineContent={Text("Protocol Updates")}, supportingContent={Text("مدیریت نسخه پروتکل‌ها")})
        ListItem(headlineContent={Text("Security")}, supportingContent={Text("کلید AI داخل UI ذخیره نمی‌شود")})
    }
}
@Composable private fun SettingSwitch(t:String,v:Boolean,on:(Boolean)->Unit){ ListItem(headlineContent={Text(t)}, trailingContent={Switch(v,on)}) }

@Composable private fun DevicePickerScreen(state: RemoteUiState, vm: RemoteViewModel) { DevicesScreen(state, vm) {} }
@Composable private fun ConnectionProgressScreen(phase: ConnectionPhase) { val msg = when(phase){ is ConnectionPhase.Discovering->"در حال پیدا کردن تلویزیون…"; is ConnectionPhase.Connecting->phase.label; else->"در حال آماده‌سازی"}; Column(Modifier.fillMaxSize().padding(28.dp),Arrangement.Center,Alignment.CenterHorizontally){ Text("Connection Wizard",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(14.dp));Text(msg);Spacer(Modifier.height(14.dp));LinearProgressIndicator(Modifier.fillMaxWidth()) } }
@Composable private fun ConnectionErrorScreen(message:String,retry:()->Unit){ Column(Modifier.fillMaxSize().padding(24.dp),Arrangement.Center,Alignment.CenterHorizontally){Text("اتصال برقرار نشد",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(10.dp));Text(message);Spacer(Modifier.height(18.dp));Button(retry){Text("تلاش مجدد")}} }
@Composable private fun PairingScreen(state:RemoteUiState,vm:RemoteViewModel){
    val androidTv = state.selectedDevice?.protocol == "androidtv-remote-v2"
    Column(Modifier.fillMaxSize().padding(24.dp),Arrangement.Center,Alignment.CenterHorizontally){
        Text("احراز هویت لازم است",style=MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(if (androidTv) "کد ۶ کاراکتری نمایش‌داده‌شده روی تلویزیون را وارد کنید." else "مقدار احراز هویت این تلویزیون را وارد کنید.")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(state.pairingCode,{vm.updatePairingCode(it)},label={Text(if(androidTv) "کد ۶ رقمی/هگز" else "کلید احراز هویت")}, singleLine=true)
        Spacer(Modifier.height(12.dp))
        Button({vm.submitPairingCode()}, enabled=state.pairingCode.isNotBlank()){Text("تأیید") }
    }
}
@Composable private fun ErrorSnackbar(message:String,onDismiss:()->Unit){ Box(Modifier.fillMaxSize().padding(16.dp),contentAlignment=Alignment.BottomCenter){Snackbar(action={TextButton(onDismiss){Text("باشه")}}){Text(message)}} }
@Composable private fun EmptyState(a:String,b:String){ Column(Modifier.fillMaxWidth().padding(48.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(a,style=MaterialTheme.typography.titleMedium);Text(b,style=MaterialTheme.typography.bodySmall)} }
@Composable private fun SectionLabel(t:String){Text(t,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(20.dp,18.dp,20.dp,8.dp))}
