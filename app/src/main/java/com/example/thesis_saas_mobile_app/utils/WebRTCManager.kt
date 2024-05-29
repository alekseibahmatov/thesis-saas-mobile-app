package com.example.thesis_saas_mobile_app.utils

import android.content.Context
import android.media.AudioManager
import android.util.Log
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule


class WebRTCManager(private val context: Context, private val socket: Socket, private val channel: String) {
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private val peerConnections: MutableMap<String, PeerConnection> = mutableMapOf()
    private lateinit var localAudioSource: AudioSource
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var audioDevice: AudioDeviceModule

    private var _channel = channel

    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var socketUserId: String? = null

    init {
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true

        initializePeerConnectionFactory()
        createAudioComponents()
        disableMicrophone() // Disable microphone by default

    }

    private fun initializePeerConnectionFactory() {
        audioDevice = JavaAudioDeviceModule.builder(context).setUseHardwareAcousticEchoCanceler(false).setUseHardwareNoiseSuppressor(false).createAudioDeviceModule()

        val options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        peerConnectionFactory = PeerConnectionFactory.builder().setAudioDeviceModule(audioDevice).createPeerConnectionFactory()
    }

    private fun createAudioComponents() {
        val audioConstraints = MediaConstraints().apply {
            optional.add(MediaConstraints.KeyValuePair("googAutoGainControl", "false"))
            optional.add(MediaConstraints.KeyValuePair("googAutoGainControl2", "false"))
            optional.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
            optional.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "false"))
            optional.add(MediaConstraints.KeyValuePair("googHighpassFilter", "false"))
            optional.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("echoCancellation", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "false"))

        }
        localAudioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", localAudioSource)
    }

    fun createPeerConnection(to: String, iceServers: List<PeerConnection.IceServer>): PeerConnection {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL

        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d("WebRTC", "ICE Candidate generated: $candidate")
                val json = JSONObject().apply {
                    put("type", "candidate")
                    put("channel", _channel)
                    put("to", to)
                    put("from", socketUserId)
                    put("signal", JSONObject().apply {
                        put("id", candidate.sdpMid)
                        put("label", candidate.sdpMLineIndex)
                        put("candidate", candidate.sdp)
                    })
                }
                socket.emit("signal", json)
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                when(state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume, 0)
                    }
                    else -> {}
                }
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                Log.d("WebRTC", "onIceGatheringChange: $state")
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {
                Log.d("WebRTC", "onSignalingChange: $state")
            }

            override fun onAddStream(stream: MediaStream) {
                Log.d("WebRTC", "onAddStream")
            }

            override fun onRemoveStream(stream: MediaStream) {
                Log.d("WebRTC", "onRemoveStream")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                Log.d("WebRTC", "onDataChannel")
            }

            override fun onRenegotiationNeeded() {
                Log.d("WebRTC", "onRenegotiationNeeded")
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d("WebRTC", "onIceCandidatesRemoved")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d("WebRTC", "onIceConnectionReceivingChange: $receiving")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d("WebRTC", "onAddTrack")
            }
        })!!

        Log.d("ConnectionPeer", peerConnection.toString())

        val stream = peerConnectionFactory.createLocalMediaStream("localStream")
        stream.addTrack(localAudioTrack)
        peerConnection.addStream(stream)

        peerConnections[to] = peerConnection
        return peerConnection
    }

    fun createOffer(to: String) {
        val peerConnection = peerConnections[to] ?: createPeerConnection(to, listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()))
        Log.d("ConnectionPeer", peerConnection.toString())
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                peerConnection.setLocalDescription(this, sessionDescription)

                val json = JSONObject().apply {
                    put("type", "offer")
                    put("channel", _channel)
                    put("to", to)
                    put("from", socketUserId)
                    put("signal", JSONObject().apply {
                        put("sdp", sessionDescription?.description)
                    })
                }
                socket.emit("signal", json)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    fun handleSignalingMessage(data: JSONObject) {
        val from = data.getString("from")
        val signalType = data.getString("type")
        val peerConnection = peerConnections[from] ?: createPeerConnection(from, listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()))
        Log.d("ConnectionPeer", peerConnection.toString())

        when (signalType) {
            "offer" -> {
                val sdp = data.getJSONObject("signal").getString("sdp")
                peerConnection.setRemoteDescription(createSdpObserver(from), SessionDescription(SessionDescription.Type.OFFER, sdp))
                peerConnection.createAnswer(createSdpObserver(from), MediaConstraints())
            }
            "answer" -> {
                val sdp = data.getJSONObject("signal").getString("sdp")
                peerConnection.setRemoteDescription(createSdpObserver(from), SessionDescription(SessionDescription.Type.ANSWER, sdp))
            }
            "candidate" -> {
                val candidate = data.getJSONObject("signal")
                peerConnection.addIceCandidate(IceCandidate(candidate.getString("id"), candidate.getInt("label"), candidate.getString("candidate")))
            }
        }
    }

    private fun createSdpObserver(from: String): SdpObserver {
        return object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                val peerConnection = peerConnections[from]
                Log.d("ConnectionPeer", peerConnection.toString())
                peerConnection!!.setLocalDescription(this, sessionDescription)
                if (sessionDescription?.type == SessionDescription.Type.ANSWER) {
                    val json = JSONObject().apply {
                        put("type", "answer")
                        put("channel", _channel)
                        put("to", from)
                        put("from", socketUserId)
                        put("signal", JSONObject().apply {
                            put("sdp", sessionDescription.description)
                        })
                    }
                    socket.emit("signal", json)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }
    }

    fun enableMicrophone() {
        audioDevice.setMicrophoneMute(false)
        audioDevice.setSpeakerMute(true)
    }

    fun disableMicrophone() {
        audioDevice.setMicrophoneMute(true)
        audioDevice.setSpeakerMute(false)
    }

    fun close() {
        peerConnections.values.forEach { it.close() }
        localAudioSource.dispose()
        peerConnectionFactory.dispose()
    }
}
