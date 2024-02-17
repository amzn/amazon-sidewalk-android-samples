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
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.viewmodel.CoverageTestEvent
import com.amazon.sidewalk.sample.viewmodel.CoverageTestUiState
import com.amazon.sidewalk.sample.viewmodel.CoverageTestViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CoverageTestStartFragment : Fragment(R.layout.fragment_coverage_test_start) {

    private val covTestViewModel by hiltNavGraphViewModels<CoverageTestViewModel>(R.id.navigation_coverage_test)
    private val args: CoverageTestStartFragmentArgs by navArgs()

    private var progressDialog: ProgressDialog? = null
    private lateinit var pingIntervalInput: EditText
    private lateinit var testDurationInput: EditText
    private lateinit var btnStartCoverageTest: Button
    private var errorDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                covTestViewModel.uiState.collect {
                    handleUiState(it)
                }
            }
        }
    }

    private fun initUI(view: View) {
        pingIntervalInput = view.findViewById(R.id.et_transmit_interval)
        testDurationInput = view.findViewById(R.id.et_test_duration)
        btnStartCoverageTest = view.findViewById(R.id.btnStartTest)

        view.findViewById<TextView>(R.id.btnStartTest).setOnClickListener {
            covTestViewModel.establishSecureConnect(args.smsn)
        }
    }

    private fun startCoverageTest() {
        val pintInterval = pingIntervalInput.text.toString().toInt()
        val testDuration = testDurationInput.text.toString().toInt()
        covTestViewModel.startCoverageTest(pintInterval, testDuration, true)
    }

    private fun handleUiState(state: CoverageTestUiState) {
        when (state) {
            is CoverageTestUiState.Idle -> Unit
            is CoverageTestUiState.Loading -> {
                val message = when (state.event) {
                    is CoverageTestEvent.Connect -> getString(R.string.connecting)
                    is CoverageTestEvent.Start -> getString(R.string.starting_coverage_test)
                    else -> getString(R.string.please_wait)
                }
                progressDialog = ProgressDialog(requireContext()).apply {
                    setMessage(message)
                    setCancelable(false)
                }
                progressDialog?.show()
            }
            is CoverageTestUiState.Connected -> {
                progressDialog?.dismiss()
                startCoverageTest()
            }
            is CoverageTestUiState.Disconnected -> {
                progressDialog?.dismiss()
                findNavController().navigateUp()
            }
            is CoverageTestUiState.Failure -> {
                progressDialog?.dismiss()
                showMessage(state.exception?.message)
            }
            is CoverageTestUiState.TestProgress -> progressDialog?.dismiss()
            is CoverageTestUiState.InProgress -> {
                progressDialog?.dismiss()
                findNavController().navigate(R.id.action_coverageTestStartFragment_to_coverageTestInProgressFragment)
            }
            else -> {}
        }
    }

    private fun showMessage(message: String?) {
        errorDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        errorDialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.error))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                findNavController().navigateUp()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errorDialog?.dismiss()
        progressDialog?.dismiss()
    }
}
