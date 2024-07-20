package com.github.shingyx.connectspeaker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.connectspeaker.R
import com.github.shingyx.connectspeaker.data.BluetoothDeviceInfo
import com.github.shingyx.connectspeaker.data.ConnectAction
import com.github.shingyx.connectspeaker.data.ConnectSpeakerClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class ShortcutActivity :
    AppCompatActivity(),
    CoroutineScope by MainScope() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // Finish ASAP, or multiple tasks could appear

        val connectAction =
            when (intent.action) {
                ACTION_TOGGLE -> ConnectAction.TOGGLE
                ACTION_CONNECT -> ConnectAction.CONNECT
                ACTION_DISCONNECT -> ConnectAction.DISCONNECT
                else ->
                    return Timber.w("Unexpected intent action ${intent.action}")
            }

        val deviceInfo = BluetoothDeviceInfo.createFromIntent(intent)

        if (deviceInfo == null || !ConnectSpeakerClient.checkBluetoothConnectPermission(this)) {
            if (deviceInfo == null) {
                updateToast(getString(R.string.select_speaker))
            }
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            return
        }

        launch { runConnectAction(deviceInfo, connectAction) }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private suspend fun runConnectAction(
        deviceInfo: BluetoothDeviceInfo,
        connectAction: ConnectAction,
    ) {
        ConnectSpeakerClient.runConnectAction(this, deviceInfo, connectAction) { progressMessage ->
            runOnUiThread {
                updateToast(progressMessage)
            }
        }
    }

    private fun updateToast(text: String) {
        toast?.cancel()
        toast =
            Toast.makeText(this, text, Toast.LENGTH_LONG).also {
                it.show()
            }
    }

    companion object {
        const val ACTION_TOGGLE = "com.github.shingyx.connectspeaker.ACTION_TOGGLE"
        const val ACTION_CONNECT = "com.github.shingyx.connectspeaker.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.github.shingyx.connectspeaker.ACTION_DISCONNECT"

        private var toast: Toast? = null

        fun createShortcutIntent(
            context: Context,
            bluetoothDeviceInfo: BluetoothDeviceInfo,
            connectAction: ConnectAction,
        ): Intent {
            val intentAction =
                when (connectAction) {
                    ConnectAction.TOGGLE -> ACTION_TOGGLE
                    ConnectAction.CONNECT -> ACTION_CONNECT
                    ConnectAction.DISCONNECT -> ACTION_DISCONNECT
                }
            val shortcutIntent = Intent(intentAction, null, context, ShortcutActivity::class.java)
            bluetoothDeviceInfo.addToIntent(shortcutIntent)
            val shortcutName = bluetoothDeviceInfo.name
            val iconRes = Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher)

            @Suppress("DEPRECATION") // Use deprecated approach for no icon badge
            return Intent().apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName)
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes)
            }
        }
    }
}
