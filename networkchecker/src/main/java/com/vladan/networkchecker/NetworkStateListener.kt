package com.vladan.networkchecker

interface NetworkStateListener {
    fun onNetworkStateChanged(networkState: NetworkState)
}