package com.haddadian.universalremote.security

import java.net.URI

/** Single source of truth for validating remote/backend repository URLs. */
object NetworkSecurityPolicy {
    fun isAllowedEndpoint(endpoint: String): Boolean = runCatching {
        val u = URI(endpoint)
        u.scheme.equals("https", true) && !u.host.isNullOrBlank() && (u.port == -1 || u.port == 443)
    }.getOrDefault(false)

    fun requireHttps(endpoint: String) {
        check(isAllowedEndpoint(endpoint)) { "Only HTTPS endpoints on the default/443 port are allowed" }
    }
}
