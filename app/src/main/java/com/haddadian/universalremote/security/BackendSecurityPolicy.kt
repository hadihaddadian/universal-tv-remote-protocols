package com.haddadian.universalremote.security

import java.net.URI

class BackendSecurityPolicy(private val approvedHosts: Set<String>) {
    fun validate(url: String): Boolean = runCatching {
        NetworkSecurityPolicy.requireHttps(url)
        val host = URI(url).host ?: return@runCatching false
        approvedHosts.any { it.equals(host, true) }
    }.getOrDefault(false)

    fun requireValid(url: String) {
        check(validate(url)) { "Unapproved AI backend endpoint" }
    }
}
