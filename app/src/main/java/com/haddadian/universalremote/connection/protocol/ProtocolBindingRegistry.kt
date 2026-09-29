package com.haddadian.universalremote.connection.protocol

import android.content.Context
import com.haddadian.universalremote.AndroidTvTransport
import com.haddadian.universalremote.LgWebOsTransport
import com.haddadian.universalremote.PhilipsJointSpaceTransport
import com.haddadian.universalremote.RokuEcpTransport
import com.haddadian.universalremote.SamsungTransport
import com.haddadian.universalremote.SonyBraviaTransport
import com.haddadian.universalremote.GenericIrTvTransport
import com.haddadian.universalremote.infra.InfraredController
import com.haddadian.universalremote.TvTransport
import com.haddadian.universalremote.security.DeviceCredentialStore
import com.haddadian.universalremote.security.DeviceCertificatePinStore
import com.haddadian.universalremote.connection.ConnectionTransport

/**
 * Executable binding registry. Declarative protocol packages select only an
 * already-compiled adapter; remote data can never provide executable code.
 */
data class ProtocolBinding(
    val protocolId: ProtocolId,
    val transport: ConnectionTransport,
    val factory: (Context) -> TvTransport
)

class ProtocolBindingRegistry(bindings: Collection<ProtocolBinding> = emptyList()) {
    private val byId: Map<ProtocolId, List<ProtocolBinding>> = bindings.groupBy { it.protocolId }

    fun bindingsFor(id: ProtocolId): List<ProtocolBinding> = byId[id].orEmpty()

    fun supports(id: ProtocolId, transport: ConnectionTransport): Boolean =
        bindingsFor(id).any { it.transport == transport }

    fun createTransport(
        id: ProtocolId,
        supportedTransports: Collection<ConnectionTransport>,
        context: Context
    ): TvTransport? = bindingsFor(id)
        .firstOrNull { it.transport in supportedTransports }
        ?.factory
        ?.invoke(context.applicationContext)

    fun all(): List<ProtocolBinding> = byId.values.flatten()

    companion object {
        fun fromBuiltIns(): ProtocolBindingRegistry = ProtocolBindingRegistry(
            listOf(
                ProtocolBinding(ProtocolId(ProtocolIds.ANDROID_TV_REMOTE_V2), ConnectionTransport.ANDROID_TV_REMOTE_V2) { AndroidTvTransport(it) },
                ProtocolBinding(ProtocolId(ProtocolIds.SAMSUNG_WS), ConnectionTransport.BRAND_PROTOCOL) { SamsungTransport(credentialStore = DeviceCredentialStore(it), pinStore = DeviceCertificatePinStore(it)) },
                ProtocolBinding(ProtocolId(ProtocolIds.LG_SSAP), ConnectionTransport.BRAND_PROTOCOL) { LgWebOsTransport(credentialStore = DeviceCredentialStore(it)) },
                ProtocolBinding(ProtocolId(ProtocolIds.SONY_IRCC), ConnectionTransport.BRAND_PROTOCOL) { SonyBraviaTransport() },
                ProtocolBinding(ProtocolId(ProtocolIds.PHILIPS_JOINTSPACE), ConnectionTransport.BRAND_PROTOCOL) { PhilipsJointSpaceTransport() },
                ProtocolBinding(ProtocolId(ProtocolIds.ROKU_ECP), ConnectionTransport.BRAND_PROTOCOL) { RokuEcpTransport() },
                ProtocolBinding(ProtocolId(ProtocolIds.GENERIC_IR_TV), ConnectionTransport.IR) { GenericIrTvTransport(InfraredController(it)) }
            )
        )
    }
}
