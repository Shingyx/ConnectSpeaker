package com.github.shingyx.connectspeaker.data.bluetootha2dp

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class BluetoothA2dpConnector(
    private val context: Context,
    private val bluetoothA2dp: BluetoothA2dp,
) {
    suspend fun connectDevice(
        device: BluetoothDevice,
        timeout: Long,
    ): Boolean = execute(device, timeout, true)

    suspend fun disconnectDevice(
        device: BluetoothDevice,
        timeout: Long,
    ): Boolean = execute(device, timeout, false)

    private suspend fun execute(
        device: BluetoothDevice,
        timeout: Long,
        shouldConnect: Boolean,
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()

        val stateReceiver =
            BluetoothA2dpConnectionStateReceiver { connected, stateDevice ->
                if (connected == shouldConnect && stateDevice == device) {
                    deferred.complete(true)
                }
            }
        ContextCompat.registerReceiver(
            context,
            stateReceiver,
            BluetoothA2dpConnectionStateReceiver.intentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val methodName = if (shouldConnect) CONNECT_METHOD_NAME else DISCONNECT_METHOD_NAME
        if (!invokeBluetoothA2dpMethod(device, methodName)) {
            deferred.complete(false)
        }

        val result =
            withTimeoutOrNull(timeout) {
                deferred.await()
            } ?: false
        context.unregisterReceiver(stateReceiver)
        return result
    }

    private fun invokeBluetoothA2dpMethod(
        device: BluetoothDevice,
        connectMethodName: String,
    ): Boolean =
        try {
            // the A2DP connection methods are hidden in the public API.
            val connectMethod =
                BluetoothA2dp::class.java.getDeclaredMethod(
                    connectMethodName,
                    BluetoothDevice::class.java,
                )
            val initiated = connectMethod.invoke(bluetoothA2dp, device)
            initiated == true
        } catch (e: Exception) {
            Timber.e(e, "Exception calling $connectMethodName")
            false
        }

    companion object {
        private const val CONNECT_METHOD_NAME = "connect"
        private const val DISCONNECT_METHOD_NAME = "disconnect"
    }
}
