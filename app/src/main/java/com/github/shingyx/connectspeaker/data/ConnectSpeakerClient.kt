package com.github.shingyx.connectspeaker.data

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.StringRes
import com.github.shingyx.connectspeaker.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

object ConnectSpeakerClient {
    @Volatile
    private var inProgress = false

    suspend fun toggleConnection(
        context: Context,
        deviceInfo: BluetoothDeviceInfo,
        reportProgress: (String) -> Unit,
    ) {
        if (inProgress) {
            Timber.w("Toggling already in progress")
            return reportProgress(
                context.getString(R.string.error_toggling_already_in_progress, deviceInfo.name),
            )
        }

        inProgress = true
        ConnectSpeakerClientInternal(context, deviceInfo, reportProgress).toggleConnection()
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
    private val reportProgress: (String) -> Unit,
) {
    suspend fun toggleConnection() {
        try {
            withTimeout(TIMEOUT) {
                toggleConnectionNoTimeout()
            }
        } catch (e: Exception) {
            val messageResId = when (e) {
                is ExceptionWithStringRes -> e.stringResId
                is TimeoutCancellationException -> R.string.error_timed_out
                else -> R.string.error_unknown
            }
            reportProgressWithResId(messageResId)
        }
    }

    private suspend fun toggleConnectionNoTimeout() {
        reportProgressWithResId(R.string.toggling_connection_with_speaker)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()?.takeIf { it.isEnabled }
            ?: throw ExceptionWithStringRes("Bluetooth disabled", R.string.error_bluetooth_disabled)

        val device = bluetoothAdapter.bondedDevices.find { it.address == deviceInfo.address }
            ?: throw ExceptionWithStringRes("Speaker not paired", R.string.error_speaker_unpaired)

        val bluetoothA2dp = getBluetoothA2dpService(bluetoothAdapter)

        val isConnected = bluetoothA2dp.connectedDevices.contains(device)
        Timber.d("$deviceInfo isConnected=$isConnected")

        val bluetoothA2dpConnector = BluetoothA2dpConnector(bluetoothA2dp)
        val successfullyStarted =
            if (!isConnected) {
                reportProgressWithResId(R.string.connecting_to_speaker)
                bluetoothA2dpConnector.connectDevice(device)
                reportProgressWithResId(R.string.connected_to_speaker)
            } else {
                reportProgressWithResId(R.string.disconnecting_from_speaker)
                bluetoothA2dpConnector.disconnectDevice(device)
                reportProgressWithResId(R.string.disconnected_from_speaker)
            }

        Timber.d("successfullyStarted=$successfullyStarted")
    }

    private suspend fun getBluetoothA2dpService(bluetoothAdapter: BluetoothAdapter): BluetoothA2dp {
        val deferred = CompletableDeferred<BluetoothA2dp?>()

        bluetoothAdapter.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    if (proxy != null && profile == BluetoothProfile.A2DP) {
                        deferred.complete(proxy as BluetoothA2dp)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {}
            },
            BluetoothProfile.A2DP,
        )

        return withTimeoutOrNull(TIMEOUT / 2) { deferred.await() }
            ?: throw ExceptionWithStringRes(
                "Failed to get BluetoothA2dp",
                R.string.error_null_bluetooth_a2dp,
            )
    }

    private fun reportProgressWithResId(@StringRes messageResId: Int) {
        val progressMessage = context.getString(messageResId, deviceInfo.name)
        reportProgress(progressMessage)
    }

    companion object {
        const val TIMEOUT = 10000L
    }
}
