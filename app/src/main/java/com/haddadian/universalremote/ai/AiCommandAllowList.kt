package com.haddadian.universalremote.ai

/**
 * Compatibility facade for callers that need an allow-list API.
 * The command names themselves live in AiCommandNames so there is only
 * one policy definition for the AI command surface.
 */
class AiCommandAllowList {
    fun isAllowed(name: String): Boolean = name.uppercase() in AiCommandNames.allowed
    fun requireAllowed(name: String) {
        check(isAllowed(name)) { "Command outside TV-control allow-list" }
    }
}
