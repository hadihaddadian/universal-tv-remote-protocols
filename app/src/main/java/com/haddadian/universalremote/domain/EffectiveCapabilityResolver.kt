package com.haddadian.universalremote.domain

/** Computes capabilities using one canonical enum representation. */
object EffectiveCapabilityResolver {
    fun resolve(protocol: Set<String>, observed: Set<String>): Set<String> =
        resolveCanonicalCapabilities(protocol.toCapabilities(), observed.toCapabilities()).toWireCapabilities()

    fun resolveCanonical(protocol: Set<String>, observed: Set<String>): Set<Capability> =
        resolveCanonicalCapabilities(protocol.toCapabilities(), observed.toCapabilities())

    fun resolveCanonicalCapabilities(protocol: Set<Capability>, observed: Set<Capability>): Set<Capability> = when {
        protocol.isEmpty() -> observed
        observed.isEmpty() -> protocol
        else -> protocol.intersect(observed)
    }
}
