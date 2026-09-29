package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.TvFingerprint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap

/**
 * Canonical owner of protocol lifecycle and resolution.
 *
 * Resolution order: built-in APK catalog -> verified local cache -> remote repository.
 * Remote refresh never blocks the connection path.
 */
class ProtocolManager(
    private val builtInRepository: ProtocolRepository,
    private val remoteRepository: ProtocolRepository?,
    private val store: ProtocolStore,
    private val validator: ProtocolPackageValidator,
    private val selector: ProtocolSelector = ProtocolSelector(),
    private val refreshScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val refreshTimestamps = ConcurrentHashMap<ProtocolId, Long>()
    private val refreshIntervalMs = 24L * 60L * 60L * 1000L

    private val _state = MutableStateFlow(ProtocolManagerState())
    val state: StateFlow<ProtocolManagerState> = _state.asStateFlow()

    suspend fun initialize() {
        _state.value = _state.value.copy(loading = true, error = null)
        val installed = runCatching { store.list() }.getOrElse {
            _state.value = _state.value.copy(loading = false, error = it.message)
            return
        }
        _state.value = _state.value.copy(
            initialized = true,
            loading = false,
            installed = installed.map { it.profile }
        )
    }

    suspend fun prepareFor(fingerprint: TvFingerprint): ProtocolManagerResult<ProtocolProfile> {
        if (!_state.value.initialized) initialize()

        val installed = runCatching { store.list() }.getOrElse {
            return failure(ProtocolManagerError.Storage("Unable to read protocol store", it))
        }

        // Built-in and installed packages participate in the same selector. This avoids
        // a generic cached package outranking a specific built-in profile.
        val localPackages = buildList {
            addAll(installed)
            builtInRepository.findCompatible(
                fingerprint.manufacturer,
                fingerprint.platform,
                fingerprint.model
            )?.let { add(it) }
        }.distinctBy { it.manifest.id to it.manifest.version }

        val localMatch = selector.select(fingerprint, localPackages.map { it.profile })
        if (localMatch != null) {
            val selected = localPackages
                .filter { it.profile == localMatch.profile }
                .maxByOrNull { it.manifest.version }
                ?: return failure(ProtocolManagerError.InvalidPackage("Selected profile package missing"))

            val validation = validator.validate(
                selected,
                allowBuiltIn = selected.manifest.trust == ProtocolTrust.BUILTIN
            )
            if (validation is ProtocolManagerResult.Failure) return validation

            _state.value = _state.value.copy(
                activeProtocol = selected.profile,
                installed = installed.map { it.profile },
                error = null
            )

            // Built-in APK protocols are self-contained and must not require the
            // remote repository on the connection path. Remote refresh is reserved
            // for installed/cache-resolved packages.
            if (selected.manifest.source != ProtocolSource.BUILTIN) {
                scheduleRemoteRefresh(selected, fingerprint)
            }
            return ProtocolManagerResult.Success(selected.profile)
        }

        // Only reach the network when the APK and local cache cannot resolve the device.
        val remote = remoteRepository?.findCompatible(
            fingerprint.manufacturer,
            fingerprint.platform,
            fingerprint.model
        ) ?: return failure(
            ProtocolManagerError.NotFound(
                ProtocolId((fingerprint.platform ?: fingerprint.manufacturer ?: "unknown")
                    .lowercase()
                    .replace(" ", "-"))
            )
        )

        val validation = validator.validate(remote, allowBuiltIn = false)
        if (validation is ProtocolManagerResult.Failure) return validation

        val current = store.get(remote.manifest.id)
        if (current == null || remote.manifest.version > current.manifest.version) {
            val installedResult = install(remote)
            if (installedResult is ProtocolManagerResult.Failure) return installedResult
        }

        val active = store.get(remote.manifest.id) ?: remote
        _state.value = _state.value.copy(
            activeProtocol = active.profile,
            installed = store.list().map { it.profile },
            error = null
        )
        return ProtocolManagerResult.Success(active.profile)
    }

    suspend fun install(packageValue: ProtocolPackage): ProtocolManagerResult<Unit> {
        val validation = validator.validate(packageValue, allowBuiltIn = false)
        if (validation is ProtocolManagerResult.Failure) {
            _state.value = _state.value.copy(error = validation.error.message)
            return validation
        }

        val current = store.get(packageValue.manifest.id)
        if (current != null) {
            if (current.manifest.payloadSha256.equals(packageValue.manifest.payloadSha256, true)) {
                return ProtocolManagerResult.Success(Unit)
            }
            if (current.manifest.version >= packageValue.manifest.version) {
                return failure(ProtocolManagerError.Incompatible("downgrade or same-version replacement rejected"))
            }
        }

        return runCatching { store.put(packageValue) }
            .fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        installed = store.list().map { it.profile },
                        error = null
                    )
                    ProtocolManagerResult.Success(Unit)
                },
                onFailure = {
                    failure(ProtocolManagerError.Storage("Unable to install protocol", it))
                }
            )
    }

    suspend fun remove(id: ProtocolId): ProtocolManagerResult<Unit> =
        runCatching { store.remove(id) }
            .fold(
                onSuccess = {
                    _state.value = _state.value.copy(installed = store.list().map { it.profile })
                    ProtocolManagerResult.Success(Unit)
                },
                onFailure = { failure(ProtocolManagerError.Storage("Unable to remove protocol", it)) }
            )

    fun close() {
        refreshScope.cancel()
    }

    private fun scheduleRemoteRefresh(selected: ProtocolPackage, fingerprint: TvFingerprint) {
        val repository = remoteRepository ?: return
        val now = System.currentTimeMillis()
        val previous = refreshTimestamps[selected.manifest.id]
        if (previous != null && now - previous < refreshIntervalMs) return
        refreshTimestamps[selected.manifest.id] = now
        refreshScope.launch {
            runCatching {
                val remote = repository.findCompatible(
                    fingerprint.manufacturer,
                    fingerprint.platform,
                    fingerprint.model
                ) ?: return@runCatching
                val selectedIsFallback = selected.profile.manifest.manufacturer.equals("Generic", true) ||
                    selected.profile.manifest.platform in setOf("generic", "ir")
                val sameProtocol = remote.manifest.id == selected.manifest.id
                val shouldInstall = if (selectedIsFallback) {
                    true
                } else {
                    sameProtocol && remote.manifest.version > selected.manifest.version
                }
                if (!shouldInstall) return@runCatching

                if (validator.validate(remote, allowBuiltIn = false) is ProtocolManagerResult.Failure) return@runCatching
                install(remote)
            }
        }
    }

    private fun <T> failure(error: ProtocolManagerError): ProtocolManagerResult<T> =
        ProtocolManagerResult.Failure(error)
}
