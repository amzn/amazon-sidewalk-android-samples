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
import android.widget.Button
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.viewmodel.CoverageTestEvent
import com.amazon.sidewalk.sample.viewmodel.CoverageTestUiState
import com.amazon.sidewalk.sample.viewmodel.CoverageTestViewModel
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CoverageTestInProgressFragment : Fragment(R.layout.fragment_coverage_test_in_progress) {
    private val covTestViewModel by hiltNavGraphViewModels<CoverageTestViewModel>(R.id.navigation_coverage_test)

    private lateinit var progressTimer: LinearProgressIndicator
    private var progressDialog: ProgressDialog? = null
    private var errorDialog: AlertDialog? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        progressTimer = view.findViewById(R.id.progress_timer)
        handleOnBackPress()
        view.findViewById<Button>(R.id.btn_stop).setOnClickListener {
            covTestViewModel.stopCoverageTest()
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
        showHideProgressDialog(state)
        when (state) {
            is CoverageTestUiState.Idle -> Unit
            is CoverageTestUiState.Failure -> {
                showErrorDialog(state.exception?.message)
            }
            is CoverageTestUiState.TestProgress -> {
                progressTimer.max = state.maxTime.toInt()
                progressTimer.progress = state.eventTimeMs.toInt()
            }
            is CoverageTestUiState.TestCompleted -> {
                findNavController().navigate(R.id.action_coverageTestInProgressFragment_to_coverageTestReportFragment)
            }
            else -> Unit
        }
    }

    private fun showHideProgressDialog(state: CoverageTestUiState) {
        progressDialog?.dismiss()
        if (state is CoverageTestUiState.Loading) {
            val message =
                when (state.event) {
                    is CoverageTestEvent.Stop -> getString(R.string.stopping_coverage_test)
                    is CoverageTestEvent.Disconnect -> getString(R.string.disconnecting)
                    else -> getString(R.string.please_wait)
                }
            progressDialog =
                ProgressDialog(requireContext()).apply {
                    setMessage(message)
                    setCancelable(false)
                }
            progressDialog?.show()
        }
    }

    private fun handleOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            covTestViewModel.stopCoverageTest()
        }
    }

    private fun showErrorDialog(message: String?) {
        errorDialog =
            AlertDialog
                .Builder(requireContext())
                .setTitle(getString(R.string.error))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    findNavController().navigate(R.id.action_global_dashBoardFragment)
                }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errorDialog?.dismiss()
        progressDialog?.dismiss()
    }
}
