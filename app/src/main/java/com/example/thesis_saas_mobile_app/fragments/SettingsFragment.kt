package com.example.thesis_saas_mobile_app.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.thesis_saas_mobile_app.LoginActivity
import com.example.thesis_saas_mobile_app.MainActivity
import com.example.thesis_saas_mobile_app.adapters.BluetoothDeviceAdapter
import com.example.thesis_saas_mobile_app.adapters.Channel
import com.example.thesis_saas_mobile_app.adapters.ChannelsAdapter
import com.example.thesis_saas_mobile_app.databinding.FragmentSettingsBinding
import com.example.thesis_saas_mobile_app.utils.BTManager
import com.example.thesis_saas_mobile_app.utils.SharedPreferencesManager
import com.example.thesis_saas_mobile_app.utils.factory.BTManagerFactory

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var btManager: BTManager
    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
    private var handler: Handler? = null
    private var animationRunnable: Runnable? = null
    private var animationState = 0

    private lateinit var ptt1ChannelAdapter: ChannelsAdapter
    private lateinit var ptt2ChannelAdapter: ChannelsAdapter

    private val channels: MutableList<Channel> = mutableListOf(
        Channel("Managers", "123"),
        Channel("Maintainers", "321"),
        Channel("Supply", "222")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = BTManagerFactory(requireContext())
        btManager = ViewModelProvider(requireActivity(), factory)[BTManager::class.java]

        setupSpinner()
        setupObservers()
        setupButtons()
        setupPTTSpinners()

        binding.logOutBTN.setOnClickListener {
            SharedPreferencesManager.removeToken(requireContext())
            startActivity(Intent(requireContext(), LoginActivity::class.java))

        }
    }

    private fun setupSpinner() {
        bluetoothDeviceAdapter = BluetoothDeviceAdapter(requireContext(), btManager.getBondedDevices()?.toMutableList() ?: mutableListOf())
        binding.DevicesSPIN.adapter = bluetoothDeviceAdapter
        binding.DevicesSPIN.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    binding.connectBtn.isEnabled = false
                } else {
                    btManager.availableDevices.value?.let { deviceList ->
                        if (position <= deviceList.size) {
                            btManager.selectedDevice = deviceList[position - 1]
                            binding.connectBtn.isEnabled = true
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupPTTSpinners() {
        ptt1ChannelAdapter = ChannelsAdapter(requireContext(), channels)
        binding.ptt1ChannelSPIN.adapter = ptt1ChannelAdapter
        binding.ptt1ChannelSPIN.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        ptt2ChannelAdapter = ChannelsAdapter(requireContext(), channels)
        binding.ptt2ChannelSPIN.adapter = ptt2ChannelAdapter
        binding.ptt2ChannelSPIN.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupObservers() {
        btManager.availableDevices.observe(viewLifecycleOwner) { devicesList ->
            bluetoothDeviceAdapter.updateDevices(devicesList)
        }

        btManager.discovering.observe(viewLifecycleOwner) { isDiscovering ->
            binding.refreshBtn.isEnabled = !isDiscovering
            binding.DevicesSPIN.setOnTouchListener { _, _ -> isDiscovering }
            binding.DevicesSPIN.alpha = if (isDiscovering) 0.5f else 1.0f
        }

        btManager.connectedDevice.observe(viewLifecycleOwner) { device ->
            binding.connectedToET.text = device?.let { "Connected to ${it.name}" }
            binding.connectBtn.isEnabled = true
        }

        btManager.connecting.observe(viewLifecycleOwner) { isConnecting ->
            if (isConnecting) {
                startConnectingAnimation()
            } else {
                stopConnectingAnimation()
            }
        }
    }

    private fun setupButtons() {
        binding.refreshBtn.setOnClickListener {
            btManager.refreshDevices()
        }

        binding.connectBtn.setOnClickListener {
            btManager.connect()
            binding.connectBtn.isEnabled = false
        }
    }

    private fun startConnectingAnimation() {
        handler = Handler(Looper.getMainLooper())
        animationRunnable = Runnable {
            when (animationState) {
                0 -> binding.connectedToET.text = "Connecting ."
                1 -> binding.connectedToET.text = "Connecting .."
                2 -> binding.connectedToET.text = "Connecting ..."
            }
            animationState = (animationState + 1) % 3
            handler?.postDelayed(animationRunnable!!, 500)
        }
        handler?.post(animationRunnable!!)
    }

    private fun stopConnectingAnimation() {
        handler?.removeCallbacks(animationRunnable!!)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}