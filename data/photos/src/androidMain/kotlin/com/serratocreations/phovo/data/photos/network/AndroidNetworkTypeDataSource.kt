package com.serratocreations.phovo.data.photos.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

internal class AndroidNetworkTypeDataSource(context: Context) : NetworkTypeDataSource {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override fun observeConnectionType(): Flow<ConnectionType> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.toConnectionType())
            }

            override fun onLost(network: Network) {
                trySend(ConnectionType.NONE)
            }
        }

        // The callback only fires on changes, so start from what is there now.
        trySend(
            connectivityManager.activeNetwork
                ?.let(connectivityManager::getNetworkCapabilities)
                ?.toConnectionType()
                ?: ConnectionType.NONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            // API 23 has no default-network callback. Any internet-capable network is the
            // closest stand-in; the app itself requires 24, so this is only for the library.
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                callback
            )
        }
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()

    // Same capabilities androidx.work requires for NetworkType.UNMETERED.
    private fun NetworkCapabilities.toConnectionType(): ConnectionType = when {
        !hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            !hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> ConnectionType.NONE
        hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> ConnectionType.UNMETERED
        else -> ConnectionType.METERED
    }
}
