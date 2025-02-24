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
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.amazon.sidewalk.sample.DeregisterDialog
import com.amazon.sidewalk.sample.IDeregisterOnClickListener
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.viewmodel.AccountSettingEvent
import com.amazon.sidewalk.sample.viewmodel.AccountSettingUiState
import com.amazon.sidewalk.sample.viewmodel.AccountSettingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AccountSettingFragment :
    Fragment(R.layout.fragment_account_setting),
    IDeregisterOnClickListener {
    private val accountSettingViewModel by viewModels<AccountSettingViewModel>()

    @Inject
    lateinit var requestContext: RequestContext

    private var progressDialog: ProgressDialog? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Start a coroutine in the lifecycle scope
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountSettingViewModel.uiState.collect {
                    handleUiState(it)
                }
            }
        }

        view.findViewById<TextView>(R.id.login).setOnClickListener {
            accountSettingViewModel.login()
        }
        view.findViewById<TextView>(R.id.logout).setOnClickListener {
            accountSettingViewModel.logout()
        }
        view.findViewById<TextView>(R.id.tv_login_status).text =
            resources.getString(
                R.string.status,
                resources.getString(R.string.status_logged_out),
            )
        view.findViewById<TextView>(R.id.tv_scan).setOnClickListener {
            activity?.onBackPressed()
        }
        view.findViewById<TextView>(R.id.tv_deregister).setOnClickListener {
            DeregisterDialog.newInstance().show(childFragmentManager, "DeregisterDialog")
        }
    }

    override fun onResume() {
        super.onResume()

        requestContext.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
    }

    private fun handleUiState(uiState: AccountSettingUiState) {
        when (uiState) {
            is AccountSettingUiState.Idle -> Unit
            is AccountSettingUiState.Loading -> {
                progressDialog =
                    ProgressDialog(requireContext()).apply {
                        setMessage("Action ${uiState.event.message}")
                        setCancelable(false)
                        setButton(
                            DialogInterface.BUTTON_NEGATIVE,
                            getString(android.R.string.cancel),
                        ) { dialog, _ ->
                            dialog.dismiss()
                        }
                    }
                progressDialog?.show()
            }
            is AccountSettingUiState.LwaToken -> {
                view?.apply {
                    findViewById<TextView>(R.id.login).visibility = View.GONE
                    findViewById<TextView>(R.id.logout).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tv_login_status).text =
                        resources.getString(
                            R.string.status,
                            resources.getString(R.string.status_logged_in),
                        )
                }
                progressDialog?.dismiss()
            }
            is AccountSettingUiState.LoggedIn -> {
                view?.apply {
                    findViewById<TextView>(R.id.login).visibility = View.GONE
                    findViewById<TextView>(R.id.logout).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tv_login_status).text =
                        resources.getString(
                            R.string.status,
                            resources.getString(R.string.status_logged_in),
                        )
                }
                progressDialog?.dismiss()
            }
            is AccountSettingUiState.LoggedOut -> {
                view?.apply {
                    findViewById<TextView>(R.id.login).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.logout).visibility = View.GONE
                    findViewById<TextView>(R.id.tv_login_status).text =
                        resources.getString(
                            R.string.status,
                            resources.getString(R.string.status_logged_out),
                        )
                }
                progressDialog?.dismiss()
            }
            is AccountSettingUiState.Deregistered -> {
                showMessage(
                    message =
                        resources.getString(
                            R.string.deregister_success_msg,
                            uiState.smsn,
                        ),
                    title = resources.getString(R.string.success),
                )
                progressDialog?.dismiss()

                // Back to previous page
                activity?.onBackPressed()
            }
            is AccountSettingUiState.Failure -> {
                val message = uiState.exception?.message ?: "null"
                when (uiState.event) {
                    is AccountSettingEvent.RequestLwaToken -> {
                        Log.e("Sidewalk", "RequestLwaToken failed, error=$message")
                    }
                    is AccountSettingEvent.Login -> {
                        Log.e("Sidewalk", "Login failed, error=$message")
                        showMessage("Login failed, error=$message")
                    }
                    is AccountSettingEvent.Logout -> {
                        Log.e("Sidewalk", "Logout failed, message=$message")
                        showMessage("Logout failed, message=$message")
                    }
                    is AccountSettingEvent.Deregister -> {
                        Log.e("Sidewalk", "Deregister failed, message=$message")
                        showMessage("Deregister failed, message=$message")
                    }
                }
                progressDialog?.dismiss()
            }
        }
    }

    private fun showMessage(
        message: String,
        title: String? = null,
    ) {
        AlertDialog
            .Builder(requireContext())
            .apply {
                if (title != null) {
                    setTitle(title)
                }
                setMessage(message)
                setPositiveButton(android.R.string.ok, null)
            }.show()
    }

    override fun onDeregisterClick(smsn: String) {
        accountSettingViewModel.deregisterDevice(smsn)
    }
}
