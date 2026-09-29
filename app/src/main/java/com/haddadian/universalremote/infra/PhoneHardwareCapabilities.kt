package com.haddadian.universalremote.infra

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.hardware.ConsumerIrManager

/**
 * Central hardware/network preflight used by discovery providers.
 * Discovery is never started for a transport whose required radio is absent
 * (or, for local Wi-Fi discovery, when there is no active Wi-Fi network).
 */
class PhoneHardwareCapabilities(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val irManager =
        appContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasWifiRadio(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

    fun hasBluetoothRadio(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

    fun hasIrEmitter(): Boolean =
        irManager?.hasIrEmitter() == true

    /** Local TV discovery currently uses Wi-Fi multicast/mDNS/SSDP. */
    fun canRunWifiDiscovery(): Boolean {
        if (!hasWifiRadio()) return false
        val network = connectivityManager?.activeNetwork ?: return false
        return connectivityManager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
