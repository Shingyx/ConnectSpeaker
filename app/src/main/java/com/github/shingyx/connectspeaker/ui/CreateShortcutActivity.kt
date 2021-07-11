package com.github.shingyx.connectspeaker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.Toast
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

        val deviceAdapter = createBluetoothDeviceAdapter()
            ?: return finish()

        binding.speakerList.adapter = deviceAdapter
        binding.speakerList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val selectedSpeaker = deviceAdapter.getItem(position)
                val intent = ShortcutActivity.createShortcutIntent(this, selectedSpeaker)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
    }

    private fun createBluetoothDeviceAdapter(): BluetoothDeviceAdapter? {
        val devicesInfo = ConnectSpeakerClient.getPairedDevicesInfo()

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
