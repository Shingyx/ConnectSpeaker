package com.github.shingyx.connectspeaker.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
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

class ConnectSpeakerClient private constructor(
    private val context: Context,
    private val deviceInfo: BluetoothDeviceInfo,
    private val reportProgress: (String) -> Unit,
) {
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private suspend fun toggleConnection() {
        try {
            withTimeout(TIMEOUT_ALL) {
                toggleConnectionInternal()
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

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private suspend fun toggleConnectionInternal() {
        reportProgressWithResId(R.string.starting)

        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter
            ?.takeIf { it.isEnabled }
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

        @Volatile
        private var inProgress = false

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
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
            val client = ConnectSpeakerClient(context, deviceInfo, reportProgress)
            client.toggleConnection()
            inProgress = false
        }

        @SuppressLint("MissingPermission")
        fun getPairedDevicesInfo(context: Context): List<BluetoothDeviceInfo>? {
            try {
                val bondedDevices = context.getSystemService(BluetoothManager::class.java).adapter
                    ?.takeIf { it.isEnabled }
                    ?.bondedDevices

                if (bondedDevices != null) {
                    return bondedDevices.map { BluetoothDeviceInfo(it.name, it.address) }.sorted()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to read bonded devices")
            }
            return null
        }

        fun checkBluetoothConnectPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
