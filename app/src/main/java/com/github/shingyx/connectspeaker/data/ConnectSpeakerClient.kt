package com.github.shingyx.connectspeaker.data

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber

object ConnectSpeakerClient {
    @Volatile
    private var inProgress = false

    suspend fun toggleConnection(
        context: Context,
        deviceInfo: BluetoothDeviceInfo,
    ) {
        if (inProgress) {
            Timber.w("Toggling already in progress")
            return
        }

        inProgress = true
        ConnectSpeakerClientInternal(context, deviceInfo).toggleConnection()
        inProgress = false
    }

    fun getPairedDevicesInfo(): List<BluetoothDeviceInfo>? {
        try {
            val bondedDevices = BluetoothAdapter.getDefaultAdapter()
                ?.takeIf { it.isEnabled }
                ?.bondedDevices

            if (bondedDevices != null) {
                return bondedDevices.map(::BluetoothDeviceInfo).sorted()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read bonded devices")
        }
        return null
    }
}

private class ConnectSpeakerClientInternal(
    private val context: Context,
    private val deviceInfo: BluetoothDeviceInfo,
) {
    private val handler = Handler(Looper.getMainLooper())

    suspend fun toggleConnection() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val bluetoothDevice = bluetoothAdapter.bondedDevices.find { it.address == deviceInfo.address }
            ?: return Timber.w(Exception("Speaker not paired"))

        val bluetoothA2dp = getBluetoothA2dpService(bluetoothAdapter)
            ?: return Timber.w(Exception("Failed to get BluetoothA2dp"))

        val isConnected = bluetoothA2dp.connectedDevices.contains(bluetoothDevice)
        Timber.d("$deviceInfo isConnected=$isConnected")
    }

    private suspend fun getBluetoothA2dpService(bluetoothAdapter: BluetoothAdapter): BluetoothA2dp? {
        val deferred = CompletableDeferred<BluetoothA2dp?>()

        val timeout = 5000L
        handler.postDelayed({ deferred.complete(null) }, timeout)

        bluetoothAdapter.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    if (proxy != null && profile == BluetoothProfile.A2DP) {
                        handler.removeCallbacksAndMessages(null)
                        deferred.complete(proxy as BluetoothA2dp)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {}
            },
            BluetoothProfile.A2DP,
        )

        return deferred.await()
    }
}
