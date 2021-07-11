package com.github.shingyx.connectspeaker.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.connectspeaker.BuildConfig
import com.github.shingyx.connectspeaker.R
import com.github.shingyx.connectspeaker.data.BluetoothStateReceiver
import com.github.shingyx.connectspeaker.data.ConnectSpeakerClient
import com.github.shingyx.connectspeaker.data.Preferences
import com.github.shingyx.connectspeaker.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var handler: Handler
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
        setSupportActionBar(binding.toolbar)

        handler = Handler(Looper.getMainLooper())
        adapter = BluetoothDeviceAdapter(this)
        bluetoothStateReceiver = BluetoothStateReceiver(this::updateBluetoothDevices)

        binding.selectSpeaker.setAdapter(adapter)
        binding.selectSpeaker.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                Preferences.bluetoothDeviceInfo = adapter.getItem(position)
                binding.toggleConnectionButton.isEnabled = true
            }
        binding.selectSpeaker.setText(Preferences.bluetoothDeviceInfo?.toString())
        binding.selectSpeaker.requestFocus()

        binding.toggleConnectionButton.isEnabled = Preferences.bluetoothDeviceInfo != null
        binding.toggleConnectionButton.setOnClickListener { launch { toggleConnection() } }

        binding.version.text = getString(R.string.version, BuildConfig.VERSION_NAME)

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

        handler.removeCallbacksAndMessages(null)

        binding.toggleConnectionButton.isEnabled = false
        fadeView(binding.progressBar, true)
        binding.progressDescription.text = ""
        fadeView(binding.progressDescription, true)

        ConnectSpeakerClient.toggleConnection(this, deviceInfo) { progressMessage ->
            runOnUiThread {
                binding.progressDescription.text = progressMessage
            }
        }

        binding.toggleConnectionButton.isEnabled = true
        fadeView(binding.progressBar, false)
        handler.postDelayed(
            { fadeView(binding.progressDescription, false) },
            4000,
        )
    }

    private fun fadeView(view: View, show: Boolean) {
        val newAlpha = if (show) 1f else 0f
        view.visibility = View.VISIBLE
        view.alpha = 1f - newAlpha
        view.animate()
            .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            .alpha(newAlpha)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    view.visibility = if (show) View.VISIBLE else View.GONE
                }
            })
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

        binding.selectSpeakerContainer.error = if (devicesInfo.isEmpty()) {
            getString(R.string.no_devices_found)
        } else {
            null
        }

        adapter.updateItems(devicesInfo)
    }
}
