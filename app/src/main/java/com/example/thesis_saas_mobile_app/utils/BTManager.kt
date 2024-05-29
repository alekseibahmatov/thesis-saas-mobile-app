package com.example.thesis_saas_mobile_app.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thesis_saas_mobile_app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BTManager(context: Context) : ViewModel() {

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val beep1: MediaPlayer = MediaPlayer.create(context, R.raw.beep1)
    private val beep2: MediaPlayer = MediaPlayer.create(context, R.raw.beep2)

    val availableDevices: MutableLiveData<List<BluetoothDevice>> by lazy { MutableLiveData(emptyList()) }
    private val _discovering = MutableLiveData<Boolean>()
    private val _connecting = MutableLiveData<Boolean>()
    val discovering: LiveData<Boolean> get() = _discovering
    val connecting: LiveData<Boolean> get() = _connecting

    var selectedDevice: BluetoothDevice? = null
    private val profileProxy: MutableSet<BluetoothProfile> = mutableSetOf()
    val connectedDevice: MutableLiveData<BluetoothDevice> = MutableLiveData(null)
    val pttPressed: MutableLiveData<Boolean> = MutableLiveData(false)
    private var bluetoothSocket: BluetoothSocket? = null

    private val registerForResult = (context as AppCompatActivity).registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Handle the Intent if needed
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                profileProxy.add(proxy)
                Log.i("BTManager", "Profile connected: HEADSET [${profileProxy.size}]")
                if (connectedDevice.value == null) {
                    try {
                        selectedDevice = proxy.connectedDevices[0]
                        connect()
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e("BTManager", "Unable to establish bluetooth connection with ${selectedDevice?.name}")
                    }
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                profileProxy.clear()
                Log.i("BTManager", "Profile disconnected: HEADSET")
            }
        }
    }

    init {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth is not available on this device", Toast.LENGTH_LONG).show()
        } else if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            registerForResult.launch(enableBtIntent)
        } else {
            bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
        }
    }

    fun startUsingBy(context: Context) {
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

    fun stopUsingBy(context: Context) {
        context.unregisterReceiver(broadcastReceiver)
    }

    private val intentFilter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> handleBondStateChanged(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE), context!!)
                BluetoothDevice.ACTION_ACL_CONNECTED -> handleAclConnected()
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> handleAclDisconnectRequested()
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleAclDisconnected()
                BluetoothDevice.ACTION_FOUND -> handleDeviceFound(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE))
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> handleDiscoveryFinished()
            }
        }
    }

    private fun handleBondStateChanged(device: BluetoothDevice?, context: Context) {
        device?.let {
            Log.i("BTManager", "Bond state changed: ${bondStateToString(it.bondState)}")
            when (it.bondState) {
                BluetoothDevice.BOND_BONDED -> openProxy(context)
                BluetoothDevice.BOND_NONE -> {
                    val updatedDevices = availableDevices.value?.filterNot { d -> d.address == it.address }
                    availableDevices.value = updatedDevices
                }
            }
        }
    }

    private fun bondStateToString(state: Int) = when (state) {
        BluetoothDevice.BOND_NONE -> "BOND_NONE"
        BluetoothDevice.BOND_BONDING -> "BOND_BONDING"
        BluetoothDevice.BOND_BONDED -> "BOND_BONDED"
        else -> "UNKNOWN"
    }

    private fun handleAclConnected() {
        Log.i("BTManager", "ACL connected")
    }

    private fun handleAclDisconnectRequested() {
        Log.i("BTManager", "ACL disconnect requested")
    }

    private fun handleAclDisconnected() {
        Log.i("BTManager", "ACL disconnected")
    }

    private fun handleDeviceFound(device: BluetoothDevice?) {
        device?.let {
            val updatedDevices = availableDevices.value?.toMutableList() ?: mutableListOf()
            updatedDevices.add(it)
            availableDevices.value = updatedDevices
        }
    }

    private fun handleDiscoveryFinished() {
        _discovering.value = false
    }

    private fun openProxy(context: Context) {
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            ?: Log.e("BTManager", "Error opening Bluetooth profile")
    }

    fun refreshDevices() {
        val bondedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        availableDevices.value = bondedDevices
        startDiscovery()
    }

    private fun startDiscovery() {
        if (bluetoothAdapter?.startDiscovery() == true) {
            _discovering.value = true
        }
    }

    fun connect() {
        if (profileProxy.isNotEmpty()) {
            val proxy = profileProxy.first() as BluetoothHeadset
            Log.i("BTManager", "Connecting to $selectedDevice")
            viewModelScope.launch(Dispatchers.IO) {
                _connecting.postValue(true)
                proxy.myConnect(selectedDevice!!)

                // Monitor connection status instead of waiting for a fixed time
                var isConnected = false
                val maxAttempts = 30 // Maximum number of attempts (each attempt = 500ms)
                var attempts = 0

                while (attempts < maxAttempts && !isConnected) {
                    delay(500) // Check every 500ms
                    isConnected = proxy.getConnectionState(selectedDevice) == BluetoothHeadset.STATE_CONNECTED
                    attempts++
                }

                withContext(Dispatchers.Main) {
                    _connecting.postValue(false)
                    if (isConnected) {
                        connectedDevice.value = selectedDevice
                        setupSocket(connectedDevice.value!!)
                    } else {
                        Log.e("BTManager", "Failed to connect to $selectedDevice")
                    }
                }
            }
        } else {
            Log.w("BTManager", "No active Bluetooth proxy")
        }
    }

    private fun setupSocket(device: BluetoothDevice) {

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket?.connect()

            Thread {
                listenForData(bluetoothSocket!!)
            }.start()
        } catch (e: IOException) {
            Log.e("BTManager", "Socket connection failed", e)
            try {
                bluetoothSocket?.close()
            } catch (closeException: IOException) {
                Log.e("BTManager", "Socket close failed", closeException)
            } finally {
                _connecting.postValue(false)
            }
        }
    }

    private fun listenForData(socket: BluetoothSocket) {
        val inputStream: InputStream = socket.inputStream
        val buffer = ByteArray(1024)
        var numBytes: Int

        while (true) {
            try {
                numBytes = inputStream.read(buffer)
                val readMessage = String(buffer, 0, numBytes)
                Log.d("BluetoothManager", "Received: $readMessage")
                when (readMessage) {
                    "+PTT=P" -> {
                        pttPressed.postValue(true)
                    }
                    "+PTT=R" -> {
                        pttPressed.postValue(false)
                        beep1.start()
                    }
                    "+PTTS=P" -> {
                        pttPressed.postValue(true)
                    }
                    "+PTTS=R" -> {
                        pttPressed.postValue(false)
                        beep2.start()
                    }
                    "+VGS=U" -> {
                        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                    }
                    "+VGS=D" -> {
                        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                    }
                    "+PTTB1=P" -> {

                    }
                    "+PTTB2=P" -> {

                    }
                }
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Input stream was disconnected", e)
                break
            }
        }
    }

    private fun BluetoothHeadset.myConnect(device: BluetoothDevice) {
        try {
            javaClass.getMethod("connect", BluetoothDevice::class.java).invoke(this, device)
        } catch (e: Exception) {
            Log.e("BTManager", "Connecting failed: ${e.message}")
        }
    }

    private fun BluetoothHeadset.myDisconnect(device: BluetoothDevice) {
        try {
            javaClass.getMethod("disconnect", BluetoothDevice::class.java).invoke(this, device)
        } catch (e: Exception) {
            Log.e("BTManager", "Disconnecting failed: ${e.message}")
        }
    }

    fun getBondedDevices(): MutableSet<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

}