package com.haddadian.universalremote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haddadian.universalremote.AuthManager
import com.haddadian.universalremote.AuthState
import com.haddadian.universalremote.Command
import com.haddadian.universalremote.Credentials
import com.haddadian.universalremote.TvDevice
import com.haddadian.universalremote.domain.ConnectionMethod
import com.haddadian.universalremote.domain.ConnectionPhase
import com.haddadian.universalremote.domain.RemoteMode
import com.haddadian.universalremote.application.MacroCatalog
import com.haddadian.universalremote.application.MacroEngine
import com.haddadian.universalremote.application.MacroCommandExecutor
import com.haddadian.universalremote.infra.DiscoveryCoordinator
import com.haddadian.universalremote.reliability.RemoteCommandDispatcher
import com.haddadian.universalremote.connection.protocol.ProtocolManagerFactory
import com.haddadian.universalremote.connection.protocol.ProtocolTransportExecutor
import com.haddadian.universalremote.connection.protocol.ProtocolIds
import com.haddadian.universalremote.BuildConfig
import com.haddadian.universalremote.reliability.RetryPolicy
import com.haddadian.universalremote.reliability.CommandPolicyEngine
import com.haddadian.universalremote.reliability.ConnectionRecoveryCoordinator
import com.haddadian.universalremote.reliability.NetworkMonitor
import com.haddadian.universalremote.security.DeviceCredentialStore
import com.haddadian.universalremote.reliability.ConnectionStateMachine
import com.haddadian.universalremote.reliability.ConnectionHealth
import com.haddadian.universalremote.reliability.ConnectionHealthMonitor
import com.haddadian.universalremote.reliability.ReliableConnectionState
import com.haddadian.universalremote.domain.toCapabilities
import com.haddadian.universalremote.domain.Capability
import com.haddadian.universalremote.settings.AppSettings
import com.haddadian.universalremote.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/** Single UI state owner. Composables only render state and emit user intents. */
data class RemoteUiState(
    val devices: List<TvDevice> = emptyList(),
    val selectedDevice: TvDevice? = null,
    val method: ConnectionMethod = ConnectionMethod.AUTO,
    val phase: ConnectionPhase = ConnectionPhase.Idle,
    val pairingCode: String = "",
    val mode: RemoteMode = RemoteMode.DPAD,
    val lastError: String? = null,
    val capabilities: RemoteUiCapabilities = RemoteUiCapabilities(false, false, false, false, false, false, false, false, false, false, false, false, false),
    val settings: AppSettings = AppSettings(),
    val health: ConnectionHealth = ConnectionHealth.UNKNOWN,
    val favoriteApps: Set<String> = emptySet(),
    val lastLatencyMs: Long? = null,
    val favoriteDevices: Set<String> = emptySet(),
    val macros: List<com.haddadian.universalremote.domain.RemoteMacro> = MacroCatalog.presets,
    val voiceActive: Boolean = false
)

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val discovery = DiscoveryCoordinator(app)
    private val auth = AuthManager(app.applicationContext)
    private val protocolRuntime = ProtocolManagerFactory.createRuntime(app.applicationContext, BuildConfig.VERSION_CODE)
    private val protocolExecutor = ProtocolTransportExecutor(protocolRuntime.manager, protocolRuntime.bindings, app.applicationContext)
    private val commandDispatcher = RemoteCommandDispatcher()
    private val connectionStateMachine = ConnectionStateMachine()
    private val settingsRepository = AppSettingsRepository(app.applicationContext)
    private val retryPolicy = RetryPolicy()
    private val commandPolicy = CommandPolicyEngine()
    private val recovery = ConnectionRecoveryCoordinator(retryPolicy)
    private val credentialStore = DeviceCredentialStore(app.applicationContext)
    private val networkMonitor = NetworkMonitor(app.applicationContext)
    private val healthMonitor = ConnectionHealthMonitor()
    private val prefs = app.applicationContext.getSharedPreferences("remote_favorites", 0)
    private val _state = MutableStateFlow(RemoteUiState(
        settings = settingsRepository.load(),
        favoriteApps = prefs.getStringSet("apps", emptySet())?.toSet().orEmpty(),
        favoriteDevices = prefs.getStringSet("devices", emptySet())?.toSet().orEmpty()
    ))
    val state: StateFlow<RemoteUiState> = _state.asStateFlow()

    fun discover() {
        if (connectionStateMachine.state == ReliableConnectionState.Discovering) return
        setPhase(ConnectionPhase.Discovering, ReliableConnectionState.Discovering)
        viewModelScope.launch {
            runCatching { discovery.discover() }
                .onSuccess { devices -> _state.update { it.copy(devices = devices) }; setPhase(ConnectionPhase.Idle, ReliableConnectionState.Idle) }
                .onFailure { e -> setPhase(ConnectionPhase.Failed(e.message ?: "جستجو ناموفق بود"), ReliableConnectionState.Failed(e.message ?: "جستجو ناموفق بود")) }
        }
    }

    fun select(device: TvDevice) {
        val method = defaultMethod(device)
        if (!connectionStateMachine.transitionTo(ReliableConnectionState.DeviceFound) &&
            connectionStateMachine.state != ReliableConnectionState.DeviceFound) {
            _state.update { it.copy(lastError = "انتخاب دستگاه از وضعیت فعلی اتصال ممکن نیست") }
            return
        }
        _state.update { it.copy(
            selectedDevice = device,
            capabilities = RemoteUiCapabilities.fromCanonical(device.capabilities.toCapabilities()),
            method = method,
            phase = ConnectionPhase.Selecting(method),
            lastError = null,
            health = ConnectionHealth.UNKNOWN
        ) }
    }

    fun chooseMethod(method: ConnectionMethod) = _state.update {
        it.copy(method = method, phase = ConnectionPhase.Selecting(method), lastError = null)
    }

    fun beginConnection() {
        val snapshot = _state.value
        val device = snapshot.selectedDevice ?: return
        if (connectionStateMachine.state in setOf(
                ReliableConnectionState.Identifying,
                ReliableConnectionState.ResolvingProtocol,
                ReliableConnectionState.PairingRequired,
                ReliableConnectionState.Pairing,
                ReliableConnectionState.Authenticating,
                ReliableConnectionState.Connecting
            ) || connectionStateMachine.state is ReliableConnectionState.Reconnecting) return

        setPhase(ConnectionPhase.Connecting(snapshot.method, 1, 3, "بررسی قابلیت‌های دستگاه…"), ReliableConnectionState.Identifying, clearError = true)
        setPhase(ConnectionPhase.Connecting(snapshot.method, 1, 3, "بررسی قابلیت‌های دستگاه…"), ReliableConnectionState.ResolvingProtocol)
        viewModelScope.launch {
            protocolExecutor.prepare(device).onSuccess { refreshEffectiveCapabilities(device) }.onFailure { e ->
                setPhase(ConnectionPhase.Failed(e.message ?: "پروتکل دستگاه پشتیبانی نمی‌شود"), ReliableConnectionState.Failed(e.message ?: "پروتکل دستگاه پشتیبانی نمی‌شود"))
                return@launch
            }

            val authSession = auth.begin(device)
            when (authSession.state) {
                AuthState.WAITING_FOR_USER -> { connectionStateMachine.transitionTo(ReliableConnectionState.PairingRequired); connectionStateMachine.transitionTo(ReliableConnectionState.Pairing); _state.update { it.copy(phase = ConnectionPhase.Pairing(snapshot.method, authSession.message)) } }
                AuthState.NOT_REQUIRED, AuthState.AUTHENTICATED -> connectTransport(device, snapshot.method)
                AuthState.CREDENTIAL_ENTERED -> connectTransport(device, snapshot.method)
                else -> setPhase(ConnectionPhase.Failed(authSession.message), ReliableConnectionState.Failed(authSession.message))
            }
        }
    }

    fun submitPairingCode() {
        val device = _state.value.selectedDevice ?: return
        val code = _state.value.pairingCode
        val protocolId = device.protocol
        viewModelScope.launch {
            val result = auth.submitCode(device, code)
            if (result.state == AuthState.AUTHENTICATED || result.state == AuthState.CREDENTIAL_ENTERED) {
                if (code.isNotBlank() && device.protocol != ProtocolIds.ANDROID_TV_REMOTE_V2) credentialStore.put(device.id, protocolId, code)
                connectionStateMachine.transitionTo(ReliableConnectionState.Authenticating)
                connectTransport(device, _state.value.method, code)
            } else setPhase(ConnectionPhase.Failed(result.message), ReliableConnectionState.Failed(result.message))
        }
    }

    fun updatePairingCode(code: String) {
        val device = _state.value.selectedDevice
        val normalized = if (device?.protocol == ProtocolIds.ANDROID_TV_REMOTE_V2) code.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(6).uppercase()
        else code.filter { !it.isWhitespace() }.take(128)
        _state.update { it.copy(pairingCode = normalized) }
    }

    /**
     * v29 fix: previously always called transports.connect(device) with an
     * empty Credentials(), so protocols that require a pre-shared value
     * (e.g. Sony BRAVIA's PSK) could never actually connect even after a
     * "successful" pairing step. The value the person entered on the
     * pairing screen is now threaded through as both pin/token/psk so each
     * vendor transport can pick the field it needs.
     */
    private suspend fun connectTransport(device: TvDevice, method: ConnectionMethod, pairingValue: String? = null) {
        setPhase(ConnectionPhase.Connecting(method, 2, 3, "برقراری ارتباط امن…"), ReliableConnectionState.Connecting)
        val stored = ProtocolIds.credentialAliases(device.protocol).asSequence()
            .mapNotNull { credentialStore.get(device.id, it) }
            .firstOrNull()
        val value = pairingValue?.takeIf { it.isNotBlank() } ?: stored
        val credentials = when (device.protocol) {
            ProtocolIds.SONY_IRCC -> Credentials(psk = value)
            ProtocolIds.SAMSUNG_WS -> Credentials(token = value)
            ProtocolIds.LG_SSAP -> Credentials(token = value)
            else -> Credentials(pin = value, token = value, psk = value)
        }
        var effectiveDevice = device
        val first = protocolExecutor.connect(device, credentials)
        val result = if (first.isSuccess) first else {
            recovery.recover(
                maxAttempts = retryPolicy.maxAttempts,
                networkChanged = { networkMonitor.hasChanged() },
                reconnect = { protocolExecutor.connect(effectiveDevice, credentials) },
                rediscoverAndReconnect = {
                    val rediscovered = discovery.discover().firstOrNull { candidate ->
                        candidate.id == device.id ||
                            (candidate.protocol == device.protocol &&
                                candidate.manufacturer.equals(device.manufacturer, true) &&
                                candidate.name.equals(device.name, true))
                    }
                    if (rediscovered != null) effectiveDevice = rediscovered
                    protocolExecutor.connect(effectiveDevice, credentials)
                },
                onAttempt = { attempt, rediscovery ->
                    if (attempt == 1) {
                        val label = if (rediscovery) "شبکه تغییر کرده؛ جستجوی مجدد تلویزیون…" else "تلاش مجدد برای اتصال…"
                        _state.update { it.copy(phase = ConnectionPhase.Connecting(method, 2, 3, label), lastError = null) }
                    }
                }
            )
        }
        result.fold(
            onSuccess = {
                _state.update { it.copy(selectedDevice = effectiveDevice) }
                networkMonitor.markCurrent()
                setPhase(ConnectionPhase.Ready(method), ReliableConnectionState.Ready)
                healthMonitor.start(viewModelScope, { protocolExecutor.healthCheck(effectiveDevice) }) { health ->
                    _state.update { it.copy(health = health) }
                }
            },
            onFailure = { e ->
                setPhase(ConnectionPhase.Failed(e.message ?: "اتصال برقرار نشد"), ReliableConnectionState.Failed(e.message ?: "اتصال برقرار نشد"))
            }
        )
    }

    fun send(command: Command) {
        viewModelScope.launch { dispatchCommand(command) }
    }

    private suspend fun dispatchCommand(command: Command): Result<Unit> {
        val device = _state.value.selectedDevice
            ?: return Result.failure(IllegalStateException("No TV selected"))
        val started = System.nanoTime()
        val policy = commandPolicy.policy(command)
        val result = commandDispatcher.dispatch(command, policy) { outgoing ->
            protocolExecutor.send(device, outgoing)
        }
        result.onSuccess { _state.update { it.copy(lastLatencyMs = (System.nanoTime() - started) / 1_000_000) } }
            .onFailure { e -> _state.update { it.copy(lastError = e.message) } }
        return result
    }

    fun toggleFavoriteDevice(deviceId: String) {
        val updated = _state.value.favoriteDevices.toMutableSet().apply {
            if (!add(deviceId)) remove(deviceId)
        }.toSet()
        prefs.edit().putStringSet("devices", updated).apply()
        _state.update { it.copy(favoriteDevices = updated) }
    }

    fun runMacro(macroId: String) {
        val macro = _state.value.macros.firstOrNull { it.id == macroId } ?: return
        viewModelScope.launch {
            MacroEngine(object : MacroCommandExecutor {
                override suspend fun execute(command: Command): Result<Unit> = dispatchCommand(command)
            }).run(macro)
                .onFailure { e -> _state.update { it.copy(lastError = e.message) } }
        }
    }

    fun toggleFavoriteApp(appLink: String) {
        val updated = _state.value.favoriteApps.toMutableSet().apply {
            if (!add(appLink)) remove(appLink)
        }.toSet()
        prefs.edit().putStringSet("apps", updated).apply()
        _state.update { it.copy(favoriteApps = updated) }
    }

    fun setMode(mode: RemoteMode) = _state.update { it.copy(mode = mode) }

    fun reconnect() { beginConnection() }

    fun clearError() = _state.update { it.copy(lastError = null) }

    fun startVoice() {
        val device = _state.value.selectedDevice ?: return
        viewModelScope.launch {
            protocolExecutor.startVoice(device, viewModelScope)
                .onSuccess { _state.update { it.copy(voiceActive = true, lastError = null) } }
                .onFailure { e -> _state.update { it.copy(lastError = e.message) } }
        }
    }

    fun stopVoice() {
        viewModelScope.launch { protocolExecutor.stopVoice(); _state.update { it.copy(voiceActive = false) } }
    }
    fun disconnect() {
        healthMonitor.stop()
        viewModelScope.launch {
            protocolExecutor.disconnectCurrent()
            if (connectionStateMachine.state != ReliableConnectionState.Disconnected) {
                connectionStateMachine.transitionTo(ReliableConnectionState.Disconnected)
            }
            setPhase(ConnectionPhase.Idle, ReliableConnectionState.Idle)
            _state.update { it.copy(selectedDevice = null, pairingCode = "", health = ConnectionHealth.UNKNOWN) }
        }
    }

    /**
     * v29 fix: this function was called from select()/beginConnection() but was
     * never defined anywhere in the project, which is an unresolved-reference
     * compile error. Restored with the mapping implied by DeviceDiscoveryService's
     * canonical protocol IDs emitted by discovery (generic-ir-tv, roku-ecp, lg-ssap, samsung-ws, sony-ircc, philips-jointspace).
     */
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_state.value.settings)
        settingsRepository.save(updated)
        _state.update { it.copy(settings = updated) }
    }

    private fun refreshEffectiveCapabilities(device: TvDevice) {
        val c: Set<Capability> = protocolExecutor.effectiveCapabilities(device)
        if (c.isEmpty()) return
        _state.update { it.copy(capabilities = RemoteUiCapabilities.fromCanonical(c)) }
    }

    private fun setPhase(phase: ConnectionPhase, reliable: ReliableConnectionState, clearError: Boolean = false) {
        if (!connectionStateMachine.transitionTo(reliable) && connectionStateMachine.state != reliable) {
            val message = "Invalid connection state transition: ${connectionStateMachine.state} -> $reliable"
            _state.update { it.copy(phase = ConnectionPhase.Failed(message), lastError = message) }
            return
        }
        _state.update { it.copy(phase = phase, lastError = if (clearError) null else it.lastError) }
    }

    override fun onCleared() {
        healthMonitor.stop()
        viewModelScope.launch { protocolExecutor.stopVoice() }
        protocolExecutor.clear()
        protocolRuntime.manager.close()
        super.onCleared()
    }

    private fun defaultMethod(device: TvDevice): ConnectionMethod = when (device.protocol) {
        ProtocolIds.GENERIC_IR_TV -> ConnectionMethod.INFRARED
        else -> ConnectionMethod.AUTO
    }
}
