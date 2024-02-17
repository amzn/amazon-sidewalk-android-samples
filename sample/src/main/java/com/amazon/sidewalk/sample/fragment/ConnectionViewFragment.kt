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
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazon.sidewalk.message.MessageDescriptor
import com.amazon.sidewalk.message.MessageType
import com.amazon.sidewalk.message.SidewalkMessage
import com.amazon.sidewalk.result.RegistrationDetail
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.adapter.SubscribeAdapter
import com.amazon.sidewalk.sample.viewmodel.ConnectionEvent
import com.amazon.sidewalk.sample.viewmodel.ConnectionUiState
import com.amazon.sidewalk.sample.viewmodel.ConnectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConnectionViewFragment : Fragment(R.layout.fragment_connectionview) {

    private val connectionViewModel by viewModels<ConnectionViewModel>()

    private var progressDialog: ProgressDialog? = null
    private lateinit var editText: EditText
    private lateinit var subscribeList: RecyclerView
    private lateinit var registerButton: Button
    private val subscribeAdapter = SubscribeAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editText = view.findViewById(R.id.editText)
        subscribeList = view.findViewById(R.id.subscribeList)
        registerButton = view.findViewById(R.id.registerButton)
        closeConnectionOnBackPress()
        setOnClickListener(view)
        setArguments(view)
        subscribeList.apply {
            adapter = subscribeAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        // Start a coroutine in the lifecycle scope
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectionViewModel.uiState.collect {
                    handleUiState(it)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    private fun setArguments(view: View) {
        val isRegister = arguments?.getBoolean(ConnectionViewModel.ARG_REGISTERED)
        val smsn = arguments?.getString(ConnectionViewModel.ARG_SMSN)
        val messageHeader = view.findViewById<TextView>(R.id.messageHeader)
        messageHeader.text = getString(R.string.subscribe_msg_header, smsn)
        if (isRegister == true) {
            registerButton.isEnabled = false
            registerButton.isClickable = false
            ViewCompat.setBackgroundTintList(
                registerButton,
                context?.let { ContextCompat.getColorStateList(it, android.R.color.darker_gray) }
            )
        }
    }

    private fun setOnClickListener(view: View) {
        registerButton.setOnClickListener {
            connectionViewModel.registerDevice()
        }
        view.findViewById<Button>(R.id.writeButton).setOnClickListener {
            write()
        }
    }

    private fun write() {
        val message = SidewalkMessage(
            editText.text.toString().toByteArray(),
            MessageDescriptor(MessageType.Get)
        )
        connectionViewModel.write(message)
    }

    // Listener to handle device back button or Toolbar back button to close active Sidewalk Connection.
    private fun closeConnectionOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            connectionViewModel.disconnectSecureChannel()
        }
    }

    private fun showMessage(message: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showMessageAndNavigateToPreviousScreen(
        message: String,
        isRegisterSuccess: Boolean = false
    ) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                navigateToPreviousScreen(
                    isRegisterSuccess
                )
            }
            .show()
    }

    private fun navigateToPreviousScreen(isRegisterSuccess: Boolean) {
        setFragmentResult("requestKey", bundleOf("isRegister" to isRegisterSuccess))
        findNavController().navigateUp()
    }

    private fun handleUiState(uiState: ConnectionUiState) {
        when (uiState) {
            is ConnectionUiState.Idle -> Unit
            is ConnectionUiState.Loading -> {
                progressDialog = ProgressDialog(requireContext()).apply {
                    setMessage("Action ${uiState.event.message}")
                    setCancelable(false)
                    setButton(
                        DialogInterface.BUTTON_NEGATIVE,
                        getString(android.R.string.cancel)
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
                }
                progressDialog?.show()
            }
            is ConnectionUiState.Connected -> {
                progressDialog?.dismiss()
                connectionViewModel.subscribe()
            }
            is ConnectionUiState.Registered -> {
                progressDialog?.dismiss()

                when (uiState.registrationDetail) {
                    is RegistrationDetail.RegistrationSucceeded -> {
                        val message = """
                            Register succeeded after establishing a secure channel,
                            Start re-scanning devices.
                        """.trimIndent()
                        showMessageAndNavigateToPreviousScreen(message, isRegisterSuccess = true)
                    }
                    is RegistrationDetail.AlreadyRegistered -> {
                        val message = "Already registered."
                        showMessage(message)
                    }
                }
            }
            is ConnectionUiState.Written -> {
                editText.text.clear()
                subscribeAdapter.submitList(uiState.messages)
                subscribeList.scrollToPosition(subscribeAdapter.itemCount - 1)
            }
            is ConnectionUiState.Read -> {
                subscribeAdapter.submitList(uiState.messages)
                subscribeList.scrollToPosition(subscribeAdapter.itemCount - 1)
            }
            is ConnectionUiState.Disconnected -> {
                progressDialog?.dismiss()

                navigateToPreviousScreen(false)
            }
            is ConnectionUiState.Failure -> {
                progressDialog?.dismiss()

                val message = uiState.exception?.message ?: "null"
                when (uiState.event) {
                    is ConnectionEvent.Connect ->
                        showMessageAndNavigateToPreviousScreen(
                            "Establish secure channel failed, message=$message"
                        )
                    is ConnectionEvent.Register ->
                        showMessage(
                            "Register failed after establishing a secure channel, message=$message"
                        )
                    is ConnectionEvent.Write ->
                        showMessage("User write message failed, message=$message")
                    is ConnectionEvent.Subscribe ->
                        showMessage("Subscribe failed, message=$message")
                    is ConnectionEvent.Disconnect ->
                        showMessageAndNavigateToPreviousScreen(
                            "Disconnect secure channel failed, message=$message"
                        )
                }
            }
        }
    }
}
