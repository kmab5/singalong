package com.kmab.singalong.lyrics

import android.content.Context
import android.net.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class Connectivity(val online: Boolean = false, val wifi: Boolean = false) {
    fun permits(wifiOnly: Boolean) = online && (!wifiOnly || wifi)
}

class NetworkMonitor(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    val changes = callbackFlow {
        fun publish() {
            val caps = manager.getNetworkCapabilities(manager.activeNetwork)
            trySend(Connectivity(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true))
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publish()
            override fun onLost(network: Network) = publish()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = publish()
        }
        manager.registerDefaultNetworkCallback(callback)
        publish()
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
