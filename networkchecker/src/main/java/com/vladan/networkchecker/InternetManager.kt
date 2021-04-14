package com.vladan.networkchecker

import android.content.Context
import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import java.util.*

/**
 * Created by vladan on 9/30/2020
 */
class InternetManager private constructor(context: Context) : NetworkCallback() {
    companion object {
        private const val TAG = "InternetManager"
        private lateinit var mInstance: InternetManager

        @MainThread
        fun getInternetManager(context: Context):InternetManager{
            mInstance = if (::mInstance.isInitialized) mInstance else
                InternetManager(context)
            return mInstance
        }
    }

    private var mConnectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var mWifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var mNetworkRequest: NetworkRequest? = null
    private var mBuilder: NetworkRequest.Builder? = null

    private val mAliveNetworks: MutableList<Network> = ArrayList()
    private var mMaxTimeToLive: Int = -1
    private var mLosingNetwork: Network? = null
    private var mAvailableNetwork: Network? = null
    private var mIsConnected = false
    private var mNetworkType = -1
    private var mSignalStrength = 1000
    private var mLinkDnBandwidth: Int = -1
    private var mLinkUpBandwidth: Int = -1
    private var mIpV4Address: LinkAddress? = null
    private var mIpV6Address: LinkAddress? = null
    private var mLastConnectionState = false
    private val mNetworkLiveData = NetworkLiveData.get()

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        mAliveNetworks.add(network)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mIsConnected = true
            val networkCapabilities = mConnectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    mNetworkType = NetworkCapabilities.TRANSPORT_WIFI
                } else if (networkCapabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_CELLULAR
                    )
                ) {
                    mNetworkType = NetworkCapabilities.TRANSPORT_CELLULAR
                }

                mSignalStrength = mWifiManager.connectionInfo.rssi
            }
            deliverEvent()
        }

        mAvailableNetwork = network
        Log.d(
            TAG,
            "Method OnAvailable:" + "$mAliveNetworks" +
                    "\nAvailableNetwork:" + mAvailableNetwork.toString() +
                    "\nIs connected:" + mIsConnected
        )
    }

    override fun onLosing(network: Network, maxMsToLive: Int) {
        super.onLosing(network, maxMsToLive)
        mLosingNetwork = network
        Log.d(
            TAG,
            "Method OnLosing" +
                    "\nLosing network" + "$network"
        )
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        object : Thread() {
            override fun run() {
                if (network === mLosingNetwork) {
                    mLosingNetwork = null
                }
                if (mAliveNetworks.size > 0) {
                    val toRemove: MutableList<Network> = ArrayList()
                    for (aliveNetwork: Network in mAliveNetworks) {
                        if (aliveNetwork == network) {
                            toRemove.add(network)
                            Log.d(
                                TAG,
                                "Method on lost added toRemove" + "$network" + " " + currentThread().name
                            )
                        }
                    }
                    mAliveNetworks.removeAll(toRemove)
                    Log.d(
                        TAG,
                        "Method on lost removed" + "$toRemove" + "$network" + " " + currentThread().name
                    )
                }
                if (mAliveNetworks.size == 0) {
                    mIsConnected = false
                    mNetworkType = -1
                    mAvailableNetwork = null
                    mSignalStrength = 1000
                    mLinkUpBandwidth = -1
                    mLinkDnBandwidth = -1
                    deliverEvent()
                }
                Log.d(
                    TAG,
                    "Method onLost:" +
                            "\nisConnected:" + mIsConnected +
                            "\nAlive networks:" + "$mAliveNetworks"
                )
            }
        }.start()
    }

    override fun onUnavailable() {
        super.onUnavailable()
        Log.d(
            TAG,
            "onUnavailable is triggered"
        )
        //TODO This method works only for API level above 25 (introduced at 26).
        if (mAliveNetworks.size != 0) mAliveNetworks.clear()
    }

    override fun onCapabilitiesChanged(
        network: Network, networkCapabilities: NetworkCapabilities,
    ) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mIsConnected =
                ((networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )))
        }
        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            mNetworkType = NetworkCapabilities.TRANSPORT_WIFI
            mSignalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                networkCapabilities.signalStrength
            else
                mWifiManager.connectionInfo.rssi

        }
        mLinkDnBandwidth = networkCapabilities.linkDownstreamBandwidthKbps
        mLinkUpBandwidth = networkCapabilities.linkUpstreamBandwidthKbps
        if (mLastConnectionState != mIsConnected) {
            deliverEvent()
            mLastConnectionState = mIsConnected
        }

        Log.d(
            TAG,
            ("Method onCapabilitiesChanged:" +
                    "\nIsConnected:" + mIsConnected +
                    "\nNetwork capabilities:" + "$networkCapabilities")
        )
    }

    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
        super.onLinkPropertiesChanged(network, linkProperties)
        val networkCapabilities = mConnectivityManager.getNetworkCapabilities(network)
        if (networkCapabilities != null) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.d(TAG, "Link cellular properties: $linkProperties")
            }
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val linkSpeed = mWifiManager.connectionInfo.linkSpeed
                val linkFrequency = mWifiManager.connectionInfo.frequency
                val linkAddresses = linkProperties.linkAddresses
                defineIpAddress(linkAddresses)
                Log.d(
                    TAG, "Link WIFI properties: $linkProperties," +
                            "\n Link speed: $linkSpeed," +
                            "\n Link frequency: $linkFrequency," +
                            "\n V4 ip address: $mIpV4Address," +
                            "\n V6 ip address: $mIpV6Address," +
                            "\n Link addresses: $linkAddresses"
                )
            }
        }
    }

    override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
        super.onBlockedStatusChanged(network, blocked)
        //TODO This method works only for API level above 28 (introduced at 29).
    }

    fun registerInternetMonitor() {
        mBuilder = NetworkRequest.Builder()
        mNetworkRequest = mBuilder!!.build()
        mConnectivityManager.registerNetworkCallback(mNetworkRequest!!, this)
    }


    fun deliverEvent() {
        val networkState = NetworkState(
            mIsConnected,
            mNetworkType,
            mMaxTimeToLive,
            mSignalStrength,
            mLinkDnBandwidth,
            mLinkUpBandwidth
        )

        mNetworkLiveData.postValue(networkState)
    }

    private fun defineIpAddress(linkAddresses: MutableList<LinkAddress>) {
        for (linkAddress: LinkAddress in linkAddresses) {
            if ("$linkAddress".contains(".")) {
                mIpV4Address = linkAddress
            }
            if ("$linkAddress".contains(":")) {
                mIpV6Address = linkAddress
            }
        }
    }

}
