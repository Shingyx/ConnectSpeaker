package com.github.shingyx.connectspeaker.data.bluetootha2dp

import android.bluetooth.BluetoothDevice
import androidx.annotation.StringRes
import com.github.shingyx.connectspeaker.R

class ConnectStrategy(
    private val bluetoothA2dpConnector: BluetoothA2dpConnector,
) : IConnectionStrategy {
    @StringRes
    override val startingMessageResId: Int = R.string.connecting_to_speaker

    @StringRes
    override val successMessageResId: Int = R.string.connected_to_speaker

    @StringRes
    override val failureMessageResId: Int = R.string.error_connecting_to_speaker_failed

    override suspend fun connectionMethod(device: BluetoothDevice, timeout: Long): Boolean {
        return bluetoothA2dpConnector.connectDevice(device, timeout)
    }
}
