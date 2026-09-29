package com.haddadian.universalremote.connection

enum class ConnectivityPriority { BUILT_IN, CACHE, LOCAL_NETWORK, REMOTE }

class OfflineFirstPolicy {
    val priority = listOf(
        ConnectivityPriority.BUILT_IN,
        ConnectivityPriority.CACHE,
        ConnectivityPriority.LOCAL_NETWORK,
        ConnectivityPriority.REMOTE
    )
}
