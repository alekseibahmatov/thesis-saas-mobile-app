package com.example.thesis_saas_mobile_app

import android.Manifest
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.thesis_saas_mobile_app.databinding.MainLayoutBinding
import com.example.thesis_saas_mobile_app.fragments.AlertsFragment
import com.example.thesis_saas_mobile_app.fragments.SettingsFragment
import com.example.thesis_saas_mobile_app.utils.BTManager
import com.example.thesis_saas_mobile_app.utils.SharedPreferencesManager
import com.example.thesis_saas_mobile_app.utils.WebRTCManager
import com.example.thesis_saas_mobile_app.utils.checkPermissions
import com.example.thesis_saas_mobile_app.utils.factory.BTManagerFactory
import com.example.thesis_saas_mobile_app.utils.openFragment
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.PeerConnection
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainLayoutBinding
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var baseUrl: String

    private val permissions: Map<String, String> = mapOf(
        Manifest.permission.BLUETOOTH to "Please allow bluetooth",
        Manifest.permission.RECORD_AUDIO to "Allow audio record",
        Manifest.permission.BLUETOOTH_ADMIN to "bluetooth admin",
        Manifest.permission.BLUETOOTH_CONNECT to "bluetooth connect",
        Manifest.permission.ACCESS_FINE_LOCATION to "Please allow location")

    private lateinit var btManager: BTManager

    private lateinit var webRTCManager: WebRTCManager
    private lateinit var socket: Socket
    private val channel = "test"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val factory = BTManagerFactory(this)
        btManager = ViewModelProvider(this, factory)[BTManager::class.java]

        btManager.startUsingBy(this)

        registerPermissions()
        checkAllPermissions()

        baseUrl = SharedPreferencesManager.getServerIp(this)!!

        openFragment(AlertsFragment.newInstance())

        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.settingsButton -> openFragment(SettingsFragment.newInstance())
                R.id.alertsButton -> openFragment(AlertsFragment.newInstance())
            }
            true
        }

        // Initialize WebSocket
        initializeSocket()
    }

    private fun initializeSocket() {
        try {
            socket = IO.socket("http://$baseUrl:3300")
        } catch (e: URISyntaxException) {
            Log.i("Socket.IO", "Unable to connect to the websocket signaling server")
            return
        }

        socket.on(Socket.EVENT_CONNECT) {
            runOnUiThread {
                Log.d("Socket.IO", "Connected to the server")
                val joinMessage = JSONObject().apply {
                    put("channel", channel)
                }
                socket.emit("join", joinMessage)

                // Initialize WebRTC Manager after socket is connected
                initializeWebRTCManager()

                btManager.pttPressed.observe(this) {
                    if (it) {
                        webRTCManager.enableMicrophone()
                    }
                    else {
                        webRTCManager.disableMicrophone()
                    }
                }
            }
        }

        socket.on("existingClients") { args ->
            if (args.isNotEmpty()) {
                val existingClients = args[0] as JSONArray
                runOnUiThread {
                    for (i in 0 until existingClients.length()) {
                        val clientId = existingClients.getString(i)
                        webRTCManager.createPeerConnection(clientId, listOf(
                            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
                        ))
                    }
                }
            }
        }

        socket.on("userId") {args ->
            if (args.isNotEmpty()) {
                webRTCManager.socketUserId = args[0] as String
            }

        }

        socket.on("signal") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                Log.d("Socket.IO", "Message received: $data")
                runOnUiThread {
                    webRTCManager.handleSignalingMessage(data)
                }
            }
        }

        socket.on("startOffer") {args ->
            if (args.isNotEmpty()) {
                val client: String = args[0] as String
                webRTCManager.createOffer(client)
            }
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d("Socket.IO", "Disconnected from the server")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.e("Socket.IO", "Connection error, unable to connect to signaling server")
        }

        socket.connect()
    }

    private fun initializeWebRTCManager() {
        webRTCManager = WebRTCManager(this, socket, channel)
    }

    private fun registerPermissions() {
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsResult ->
            var init = true
            permissions.forEach { (key, value) ->
                if (permissionsResult[key] != true) {
                    init = false
                    Toast.makeText(this, value, Toast.LENGTH_LONG).show()
                }
            }
            if (init) {
                // All required permissions are granted
            }
        }
    }

    private fun checkAllPermissions() {
        val requestPermissions = mutableListOf<String>()

        permissions.forEach { (key, _) ->
            if (!checkPermissions(key)) {
                requestPermissions.add(key)
            }
        }

        if (requestPermissions.isNotEmpty()) {
            pLauncher.launch(requestPermissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        btManager.stopUsingBy(this)
        if (this::webRTCManager.isInitialized) {
            webRTCManager.close()
        }
        if (this::socket.isInitialized) {
            socket.disconnect()
        }
        super.onDestroy()
    }
}
