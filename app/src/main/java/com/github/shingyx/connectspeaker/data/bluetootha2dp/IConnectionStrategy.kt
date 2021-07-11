package com.github.shingyx.connectspeaker.data.bluetootha2dp

import android.bluetooth.BluetoothDevice

interface IConnectionStrategy {
    val startingMessageResId: Int
    val successMessageResId: Int
    val failureMessageResId: Int

    suspend fun connectionMethod(device: BluetoothDevice, timeout: Long): Boolean
}
