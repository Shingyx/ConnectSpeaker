package com.github.shingyx.connectspeaker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.connectspeaker.R
import com.github.shingyx.connectspeaker.data.BluetoothDeviceInfo
import com.github.shingyx.connectspeaker.data.ConnectSpeakerClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class ShortcutActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // Finish ASAP, or multiple tasks could appear

        if (intent.action != ACTION_TOGGLE) {
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
        } else {
            launch { toggleConnection(deviceInfo) }
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private suspend fun toggleConnection(deviceInfo: BluetoothDeviceInfo) {
        ConnectSpeakerClient.toggleConnection(this, deviceInfo) { progressMessage ->
            runOnUiThread {
                updateToast(progressMessage)
            }
        }
    }

    private fun updateToast(text: String) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_LONG).also {
            it.show()
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.github.shingyx.connectspeaker.ACTION_TOGGLE"

        private var toast: Toast? = null

        fun createShortcutIntent(
            context: Context,
            bluetoothDeviceInfo: BluetoothDeviceInfo,
        ): Intent {
            val shortcutIntent = Intent(ACTION_TOGGLE, null, context, ShortcutActivity::class.java)
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
