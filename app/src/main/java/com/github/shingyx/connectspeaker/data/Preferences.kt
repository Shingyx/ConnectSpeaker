package com.github.shingyx.connectspeaker.data

import android.content.Context
import android.content.SharedPreferences

object Preferences {
    private const val SHARED_PREFERENCES_NAME = "ConnectSpeakerData"
    private const val KEY_DEVICE_NAME = "DeviceName"
    private const val KEY_DEVICE_ADDRESS = "DeviceAddress"

    private lateinit var sharedPreferences: SharedPreferences

    var bluetoothDeviceInfo: BluetoothDeviceInfo?
        get() {
            val name = sharedPreferences.getString(KEY_DEVICE_NAME, null) ?: return null
            val address = sharedPreferences.getString(KEY_DEVICE_ADDRESS, null) ?: return null
            return BluetoothDeviceInfo(name, address)
        }
        set(value) {
            sharedPreferences
                .edit()
                .putString(KEY_DEVICE_NAME, value?.name)
                .putString(KEY_DEVICE_ADDRESS, value?.address)
                .apply()
        }

    fun initialize(context: Context) {
        if (!this::sharedPreferences.isInitialized) {
            sharedPreferences =
                context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }
}
