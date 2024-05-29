package com.example.thesis_saas_mobile_app.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesis_saas_mobile_app.adapters.AlertsAdapter
import com.example.thesis_saas_mobile_app.databinding.FragmentAlertsBinding
import com.example.thesis_saas_mobile_app.retrofit.api.AlertService
import com.example.thesis_saas_mobile_app.retrofit.dto.AlertResponse
import com.example.thesis_saas_mobile_app.retrofit.getRetrofitClient
import com.example.thesis_saas_mobile_app.utils.SharedPreferencesManager
import com.example.thesis_saas_mobile_app.utils.openFragment
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URISyntaxException

class AlertsFragment : Fragment() {

    private lateinit var binding: FragmentAlertsBinding
    private lateinit var recyclerView: RecyclerView

    private lateinit var alertAdapter: AlertsAdapter

    private lateinit var baseUrl: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.alertRV
        recyclerView.layoutManager = LinearLayoutManager(context)

        alertAdapter = AlertsAdapter(mutableListOf())
        recyclerView.adapter = alertAdapter

        baseUrl = SharedPreferencesManager.getServerIp(requireContext())!!

        updateAlerts()
        socketInit()
    }

    private fun updateAlerts() {
        if (!isAdded) return

        val alertService = getRetrofitClient(requireContext()).create(AlertService::class.java)
        val call = alertService.getAlerts()

        call.enqueue(object : Callback<AlertResponse> {
            override fun onResponse(p0: Call<AlertResponse>, response: Response<AlertResponse>) {
                if (!isAdded) return

                if (response.isSuccessful) {
                    val data = response.body()

                    alertAdapter.add(data?.alerts!!)
                    alertAdapter.onItemClick = {
                        openFragment(EstimationFragment.newInstance(it.machineName, it.machineId, it.alertId))
                    }
                }
            }

            override fun onFailure(p0: Call<AlertResponse>, p1: Throwable) {
                if (!isAdded) return
            }
        })
    }

    private fun socketInit() {
        lateinit var socket: Socket
        try {
            socket = IO.socket("http://$baseUrl:3330")
        } catch (e: URISyntaxException) {
            Log.i("Socket.IO", "Unable to connect to the websocket signaling server")
            return
        }

        socket.on(Socket.EVENT_CONNECT) {
            Log.d("Socket.IO", "Connected to the alert websocket")
        }

        socket.on("alert") {
            if (isAdded) {
                updateAlerts()
            }
        }

        socket.connect()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AlertsFragment()
    }
}
