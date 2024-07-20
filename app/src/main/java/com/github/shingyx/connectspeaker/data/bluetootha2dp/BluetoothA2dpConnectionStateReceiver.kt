package com.github.shingyx.connectspeaker.data.bluetootha2dp

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothA2dpConnectionStateReceiver(
    private val onStateChanged: (connected: Boolean, device: BluetoothDevice) -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (
                (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_DISCONNECTED) &&
                device != null
            ) {
                onStateChanged(state == BluetoothProfile.STATE_CONNECTED, device)
            }
        }
    }

    companion object {
        fun intentFilter(): IntentFilter = IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
    }
}
