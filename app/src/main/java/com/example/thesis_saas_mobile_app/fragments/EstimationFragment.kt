package com.example.thesis_saas_mobile_app.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import com.example.thesis_saas_mobile_app.R
import com.example.thesis_saas_mobile_app.databinding.FragmentEstimationBinding
import com.example.thesis_saas_mobile_app.retrofit.api.AlertService
import com.example.thesis_saas_mobile_app.retrofit.dto.ClaimAlertRequest
import com.example.thesis_saas_mobile_app.retrofit.dto.ClaimAlertResponse
import com.example.thesis_saas_mobile_app.retrofit.getRetrofitClient
import com.example.thesis_saas_mobile_app.utils.openFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EstimationFragment : Fragment() {

    private lateinit var binding: FragmentEstimationBinding

    private lateinit var machineId: String
    private lateinit var machineName: String
    private lateinit var alertId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            machineId = it.getString(ARG_MACHINE_ID)!!
            machineName = it.getString(ARG_MACHINE_NAME)!!
            alertId = it.getString(ARG_ALERT_ID)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEstimationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.machineNameTV.text = "Issue of $machineName"

        binding.startWorkBTN.setOnClickListener {
            val time = binding.avgTimeETN.text.toString()

            if (time == "") {
                Toast.makeText(requireContext(), "Please fill time field", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val alertService = getRetrofitClient(requireContext()).create(AlertService::class.java)
            val call = alertService.claimAlert(ClaimAlertRequest(alertId, machineId, time.toInt()))

            call.enqueue(object : Callback<ClaimAlertResponse> {
                override fun onResponse(
                    p0: Call<ClaimAlertResponse>,
                    response: Response<ClaimAlertResponse>
                ) {
                    if (response.isSuccessful) {
openFragment(JobFragment.newInstance(machineName, alertId))
                    }
                }

                override fun onFailure(p0: Call<ClaimAlertResponse>, p1: Throwable) {
                    TODO("Not yet implemented")
                }

            })
        }
    }

    companion object {
        private const val ARG_ALERT_ID = "alert_id"
        private const val ARG_MACHINE_ID = "machine_id"
        private const val ARG_MACHINE_NAME = "machine_name"
        @JvmStatic
        fun newInstance(machineName: String, machineId: String, alertId: String): EstimationFragment {
            val fragment = EstimationFragment()
            val args = Bundle()
            args.putString(ARG_MACHINE_ID, machineId)
            args.putString(ARG_MACHINE_NAME, machineName)
            args.putString(ARG_ALERT_ID, alertId)
            fragment.arguments = args
            return fragment
        }
    }
}