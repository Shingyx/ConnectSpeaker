package com.github.shingyx.connectspeaker.data

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice

class BluetoothA2dpConnector(
    private val bluetoothA2dp: BluetoothA2dp,
) {
    fun connectDevice(device: BluetoothDevice): Boolean {
        return execBluetoothA2dpMethod(device, "connect")
    }

    fun disconnectDevice(device: BluetoothDevice): Boolean {
        return execBluetoothA2dpMethod(device, "disconnect")
    }

    private fun execBluetoothA2dpMethod(
        device: BluetoothDevice,
        connectMethodName: String,
    ): Boolean {
        val connectMethod = BluetoothA2dp::class.java.getDeclaredMethod(
            connectMethodName,
            BluetoothDevice::class.java,
        )
        val initiated = connectMethod.invoke(bluetoothA2dp, device)
        return initiated == true
    }
}
