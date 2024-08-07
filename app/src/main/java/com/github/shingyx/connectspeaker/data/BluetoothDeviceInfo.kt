package com.github.shingyx.connectspeaker.data

import android.content.Intent

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
) : Comparable<BluetoothDeviceInfo> {
    override fun toString(): String = name

    override fun compareTo(other: BluetoothDeviceInfo): Int = name.compareTo(other.name, true)

    fun addToIntent(intent: Intent) {
        intent.putExtra(EXTRA_NAME, name)
        intent.putExtra(EXTRA_ADDRESS, address)
    }

    companion object {
        const val EXTRA_NAME = "com.github.shingyx.connectspeaker.EXTRA_BLUETOOTH_DEVICE_NAME"
        const val EXTRA_ADDRESS = "com.github.shingyx.connectspeaker.EXTRA_BLUETOOTH_DEVICE_ADDRESS"

        fun createFromIntent(intent: Intent): BluetoothDeviceInfo? {
            val name = intent.getStringExtra(EXTRA_NAME) ?: return null
            val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return null
            return BluetoothDeviceInfo(name, address)
        }
    }
}
