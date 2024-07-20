package com.github.shingyx.connectspeaker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.connectspeaker.R
import com.github.shingyx.connectspeaker.data.ConnectSpeakerClient
import com.github.shingyx.connectspeaker.databinding.ActivityCreateShortcutBinding
import timber.log.Timber

class CreateShortcutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateShortcutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateShortcutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (intent.action != Intent.ACTION_CREATE_SHORTCUT) {
            Timber.w("Unexpected intent action ${intent.action}")
            return finish()
        }

        if (!ConnectSpeakerClient.checkBluetoothConnectPermission(this)) {
            val intent = Intent(this, MainActivity::class.java)
            return startActivity(intent)
        }

        val adapter =
            createBluetoothDeviceAdapter()
                ?: return finish()

        binding.speakerList.adapter = adapter
        binding.speakerList.onItemClickListener =
            adapter.onItemClick { item ->
                val intent = ShortcutActivity.createShortcutIntent(this, item)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun createBluetoothDeviceAdapter(): BluetoothDeviceAdapter? {
        val devicesInfo = ConnectSpeakerClient.getPairedDevicesInfo(this)

        if (devicesInfo == null) {
            Toast.makeText(this, R.string.error_bluetooth_disabled, Toast.LENGTH_LONG).show()
            return null
        }

        if (devicesInfo.isEmpty()) {
            Toast.makeText(this, R.string.no_devices_found, Toast.LENGTH_LONG).show()
            return null
        }

        return BluetoothDeviceAdapter(this).apply {
            updateItems(devicesInfo)
        }
    }
}
