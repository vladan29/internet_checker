package com.vladan.networkchecker

/**
 * Created by vladan on 9/30/2020
 */
class NetworkState(

    var isConnected: Boolean,
    var connectionType: Int,
    var maxMsToLive: Int,
    var signalStrength: Int,
    var linkDnBandwidth: Int,
    var linkUpBandwidth: Int
)
