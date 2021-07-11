package com.github.shingyx.connectspeaker.ui

import android.os.Bundle
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.connectspeaker.R
import com.github.shingyx.connectspeaker.data.ConnectSpeakerClient
import com.github.shingyx.connectspeaker.data.Preferences
import com.github.shingyx.connectspeaker.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: BluetoothDeviceAdapter
    private lateinit var bluetoothStateReceiver: BluetoothStateReceiver

    private val bluetoothOffAlertDialog = lazy {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.bluetooth_turned_off_alert)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = BluetoothDeviceAdapter(this)
        bluetoothStateReceiver = BluetoothStateReceiver(this::updateBluetoothDevices)

        binding.selectSpeaker.setAdapter(adapter)
        binding.selectSpeaker.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Preferences.bluetoothDeviceInfo = adapter.getItem(position)
            binding.toggleConnectionButton.isEnabled = true
        }
        binding.selectSpeaker.setText(Preferences.bluetoothDeviceInfo?.toString())
        binding.selectSpeaker.requestFocus()

        binding.toggleConnectionButton.isEnabled = Preferences.bluetoothDeviceInfo != null
        binding.toggleConnectionButton.setOnClickListener { launch { toggleConnection() } }

        registerReceiver(bluetoothStateReceiver, BluetoothStateReceiver.intentFilter())
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothDevices()
        binding.selectSpeaker.dismissDropDown()
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothStateReceiver)
        super.onDestroy()
    }

    private suspend fun toggleConnection() {
        val deviceInfo = Preferences.bluetoothDeviceInfo
            ?: return Toast.makeText(this, R.string.select_speaker, Toast.LENGTH_LONG).show()

        ConnectSpeakerClient.toggleConnection(this, deviceInfo)
    }

    private fun updateBluetoothDevices() {
        var devicesInfo = ConnectSpeakerClient.getPairedDevicesInfo()

        if (devicesInfo == null) {
            bluetoothOffAlertDialog.value.show()
            devicesInfo = emptyList()
        } else {
            if (bluetoothOffAlertDialog.isInitialized()) {
                bluetoothOffAlertDialog.value.dismiss()
            }
        }

/* TODO
        select_speaker_container.error = if (devicesInfo.isEmpty()) {
            getString(R.string.no_devices_found)
        } else {
            null
        }
*/

        adapter.updateItems(devicesInfo)
    }
}
