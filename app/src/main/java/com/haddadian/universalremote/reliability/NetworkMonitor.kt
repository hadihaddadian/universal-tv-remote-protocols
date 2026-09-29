package com.haddadian.universalremote.reliability

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/** Tracks the network used by the last successful connection, not the VM creation time. */
class NetworkMonitor(context: Context) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var markedNetwork: Network? = manager.activeNetwork

    fun markCurrent() {
        markedNetwork = manager.activeNetwork
    }

    fun hasChanged(): Boolean = manager.activeNetwork != markedNetwork

    fun isConnected(): Boolean = manager.activeNetwork?.let { network ->
        manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } ?: false
}
