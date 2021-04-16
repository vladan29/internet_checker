package com.vladan.networkchecker

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData

class NetworkLiveData private constructor(): MutableLiveData<NetworkState>() {

    companion object {
        private lateinit var mInstance: NetworkLiveData

        @MainThread
        @JvmStatic
        fun get(): NetworkLiveData {
            mInstance = if (::mInstance.isInitialized) mInstance else NetworkLiveData()
            return mInstance
        }
    }
}
