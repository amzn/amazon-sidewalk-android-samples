/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazon.sidewalk.sample.fragment

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.amazon.sidewalk.coverage.SidewalkCoverageTestReport
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.viewmodel.CoverageTestEvent
import com.amazon.sidewalk.sample.viewmodel.CoverageTestUiState
import com.amazon.sidewalk.sample.viewmodel.CoverageTestViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CoverageTestReportFragment : Fragment(R.layout.fragment_coverage_test_report) {
    private val covTestViewModel by hiltNavGraphViewModels<CoverageTestViewModel>(R.id.navigation_coverage_test)

    private lateinit var reportLayout: LinearLayout
    private var progressDialog: ProgressDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleOnBackPress()
        reportLayout = view.findViewById(R.id.report)
        view.findViewById<TextView>(R.id.btn_done).setOnClickListener {
            covTestViewModel.disconnectSecureConnection()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                covTestViewModel.uiState.collect {
                    handleUiState(it)
                }
            }
        }
    }

    private fun handleUiState(state: CoverageTestUiState) {
        when (state) {
            is CoverageTestUiState.Idle -> Unit
            is CoverageTestUiState.Loading -> {
                progressDialog = ProgressDialog(requireContext()).apply {
                    setMessage(getString(R.string.reading_report))
                    setCancelable(false)
                }
                progressDialog?.show()
            }
            is CoverageTestUiState.Failure -> {
                progressDialog?.dismiss()
                if (state.event != CoverageTestEvent.Subscribe) {
                    showMessage(state.exception?.message)
                }
            }
            is CoverageTestUiState.Disconnected -> {
                progressDialog?.dismiss()
                findNavController().navigate(R.id.action_global_dashBoardFragment)
            }
            is CoverageTestUiState.ReportFetched -> {
                showReport(state.data)
                progressDialog?.dismiss()
            }
            else -> {
                progressDialog?.dismiss()
            }
        }
    }

    private fun showReport(data: SidewalkCoverageTestReport) {
        reportLayout.findViewById<TextView>(R.id.total_pkts).text =
            getString(R.string.total_pings, data.totalPkts)
        reportLayout.findViewById<TextView>(R.id.total_pong).text =
            getString(R.string.total_pongs, data.totalPongs)
        reportLayout.findViewById<TextView>(R.id.link_type).text =
            getString(R.string.link_type, getLinkType(data.linkType))
    }

    private fun getLinkType(linkType: Int): String {
        return when (linkType) {
            0 -> "LoRA"
            1 -> "FSK"
            2 -> "BLE"
            14 -> "AUTO_SUB_GHZ"
            else -> "AUTO_ALL_PHY"
        }
    }

    private fun showMessage(message: String?) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.error))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                findNavController().navigate(R.id.action_global_dashBoardFragment)
            }
            .show()
    }

    private fun handleOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigate(R.id.action_global_dashBoardFragment)
        }
    }
}
