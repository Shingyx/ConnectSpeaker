package com.github.shingyx.connectspeaker.data

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.StringRes
import com.github.shingyx.connectspeaker.R
import com.github.shingyx.connectspeaker.data.bluetootha2dp.BluetoothA2dpConnector
import com.github.shingyx.connectspeaker.data.bluetootha2dp.ConnectStrategy
import com.github.shingyx.connectspeaker.data.bluetootha2dp.DisconnectStrategy
import com.github.shingyx.connectspeaker.data.bluetootha2dp.IConnectionStrategy
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
            withTimeout(TIMEOUT_ALL) {
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
        reportProgressWithResId(R.string.starting)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()?.takeIf { it.isEnabled }
            ?: throw ExceptionWithStringRes("Bluetooth disabled", R.string.error_bluetooth_disabled)

        val device = bluetoothAdapter.bondedDevices.find { it.address == deviceInfo.address }
            ?: throw ExceptionWithStringRes("Speaker not paired", R.string.error_speaker_unpaired)

        val bluetoothA2dp = getBluetoothA2dpService(bluetoothAdapter)
        val isConnected = bluetoothA2dp.connectedDevices.contains(device)

        val bluetoothA2dpConnector = BluetoothA2dpConnector(context, bluetoothA2dp)

        val connectionStrategy = if (!isConnected) {
            ConnectStrategy(bluetoothA2dpConnector)
        } else {
            DisconnectStrategy(bluetoothA2dpConnector)
        }
        applyConnectionStrategy(connectionStrategy, device)
    }

    private suspend fun applyConnectionStrategy(
        strategy: IConnectionStrategy,
        device: BluetoothDevice,
    ) {
        reportProgressWithResId(strategy.startingMessageResId)

        val success = strategy.connectionMethod(device, TIMEOUT_CONNECT)
        if (!success) {
            throw ExceptionWithStringRes("Toggling connection failed", strategy.failureMessageResId)
        }

        reportProgressWithResId(strategy.successMessageResId)
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

        return withTimeoutOrNull(TIMEOUT_GET_SERVICE) { deferred.await() }
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
        private const val TIMEOUT_ALL = 15000L
        private const val TIMEOUT_GET_SERVICE = 5000L
        private const val TIMEOUT_CONNECT = 10000L
    }
}
