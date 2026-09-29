package com.haddadian.universalremote.connection

import java.net.InetAddress

/**
 * v29 fix: renamed from `TvTransport` to `TvTransportKind`. The old name
 * collided in spirit (same simple name, easy to import the wrong one) with
 * `RealProtocols.TvTransport` (an interface) and `domain.ProfileTransport`
 * (a differently-valued enum). None of the three actually redeclared each
 * other -- they live in different packages -- but three same-named,
 * different-shaped types is exactly the kind of confusion that produced the
 * real redeclaration bugs elsewhere in this project, so it is disambiguated
 * here too.
 */
enum class TvTransportKind { HTTPS, LOCAL_HTTP, BLUETOOTH, INFRARED, TCP }

/**
 * Centralized local-TV destination policy. Only literal RFC1918 IPv4 addresses
 * are accepted for local network transports; loopback and link-local are rejected.
 */
object TvTransportPolicy {
    fun isPrivateIpv4(value: String): Boolean {
        val host = value.removePrefix("http://").removePrefix("https://").substringBefore(":")
        return isPrivateIpv4Host(host)
    }

    fun isPrivateIpv4Host(host: String): Boolean {
        // Require a literal IPv4 address. Resolving arbitrary hostnames here
        // would create a DNS-rebinding/SSRF escape from the local-only policy.
        if (!host.matches(Regex("\\d{1,3}(?:\\.\\d{1,3}){3}"))) return false
        val octets = host.split('.').mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        // Explicitly reject loopback and link-local ranges even though they
        // do not match the RFC1918 branches below.
        if (octets[0] == 127) return false
        if (octets[0] == 169 && octets[1] == 254) return false
        return octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168)
    }
}
