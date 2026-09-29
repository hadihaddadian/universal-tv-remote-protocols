package com.haddadian.universalremote.connection.protocol

/**
 * Canonical runtime boundary for protocol resolution and executable bindings.
 * There must be exactly one ProtocolRuntime declaration in this package.
 */
class ProtocolRuntime(
    val manager: ProtocolManager,
    private val registry: ProtocolBindingRegistry
) {
    val bindings: ProtocolBindingRegistry
        get() = registry
}
