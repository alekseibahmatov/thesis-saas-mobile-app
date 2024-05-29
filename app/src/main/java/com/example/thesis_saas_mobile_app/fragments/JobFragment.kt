package com.example.thesis_saas_mobile_app.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import com.example.thesis_saas_mobile_app.R
import com.example.thesis_saas_mobile_app.databinding.FragmentJobBinding
import com.example.thesis_saas_mobile_app.retrofit.api.AlertService
import com.example.thesis_saas_mobile_app.retrofit.dto.ClaimAlertResponse
import com.example.thesis_saas_mobile_app.retrofit.dto.FinishAlertRequest
import com.example.thesis_saas_mobile_app.retrofit.getRetrofitClient
import com.example.thesis_saas_mobile_app.utils.openFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class JobFragment : Fragment() {

    private lateinit var binding: FragmentJobBinding

    private lateinit var machineName: String
    private lateinit var alertId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            machineName = it.getString(ARG_MACHINE_NAME)!!
            alertId = it.getString(ARG_ALERT_ID)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentJobBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.jobStartedTV.text = "Job started for: $machineName"
        binding.finishJobBTN.setOnClickListener {
            val alertService = getRetrofitClient(requireContext()).create(AlertService::class.java)
            val call = alertService.finishAlert(FinishAlertRequest(alertId))

            call.enqueue(object : Callback<ClaimAlertResponse> {
                override fun onResponse(
                    p0: Call<ClaimAlertResponse>,
                    response: Response<ClaimAlertResponse>
                ) {
                    if (response.isSuccessful) {
                        openFragment(AlertsFragment.newInstance())
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
        private const val ARG_MACHINE_NAME = "machine_name"
        @JvmStatic
        fun newInstance(machineName: String, alertId: String): JobFragment {
            val fragment = JobFragment()
            val args = Bundle()
            args.putString(ARG_MACHINE_NAME, machineName)
            args.putString(ARG_ALERT_ID, alertId)
            fragment.arguments = args
            return fragment
        }
    }
}