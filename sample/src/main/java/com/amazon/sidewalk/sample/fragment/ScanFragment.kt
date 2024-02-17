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

import android.Manifest
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazon.sidewalk.device.BeaconInfo
import com.amazon.sidewalk.device.SidewalkDevice
import com.amazon.sidewalk.result.RegistrationDetail
import com.amazon.sidewalk.sample.R
import com.amazon.sidewalk.sample.adapter.DeviceAdapter
import com.amazon.sidewalk.sample.adapter.SectionAdapter
import com.amazon.sidewalk.sample.viewmodel.ConnectionViewModel
import com.amazon.sidewalk.sample.viewmodel.ScanEvent
import com.amazon.sidewalk.sample.viewmodel.ScanUiState
import com.amazon.sidewalk.sample.viewmodel.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScanFragment : Fragment(R.layout.fragment_scan) {

    private val bleRuntimePermissionsLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                requestLocationPermissionIfNeeded()
            } else {
                showMessage(
                    "BLE permission denied",
                    "Please provide BLE permissions for the app to work properly."
                )
            }
        }

    private val bluetoothEnableLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                requestLocationPermissionIfNeeded()
            } else {
                showMessage(
                    "BLE permission denied",
                    "Please provide BLE permissions for the app to work properly."
                )
            }
        }

    private val locationPermissionsLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { permissions ->
            if (permissions.values.any { !it }) {
                showMessage(
                    "Location permission denied",
                    "Please provide Location permissions for the app to work properly."
                )
            }
        }

    private val scanViewModel by viewModels<ScanViewModel>()

    private val registerDeviceAdapter = DeviceAdapter { sidewalkDevice, itemId ->
        actionPerformOnDevice(sidewalkDevice, itemId)
    }

    private val unregisterDeviceAdapter = DeviceAdapter { sidewalkDevice, itemId ->
        actionPerformOnDevice(sidewalkDevice, itemId)
    }

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val concatAdapter = ConcatAdapter().apply {
            addAdapter(SectionAdapter(getString(R.string.unregistered_devices).uppercase()))
            addAdapter(unregisterDeviceAdapter)
            addAdapter(SectionAdapter(getString(R.string.registered_devices).uppercase()))
            addAdapter(registerDeviceAdapter)
        }
        view.findViewById<RecyclerView>(R.id.list).apply {
            adapter = concatAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        requestBlePermissions()

        // Start a coroutine in the lifecycle scope
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                scanViewModel.uiState.collect {
                    handleUiState(it)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        scanViewModel.scan()
    }

    override fun onStop() {
        super.onStop()
        scanViewModel.cancelScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_action, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.refresh) {
            scanViewModel.scan(force = true)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bleRuntimePermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            locationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun showMessage(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun actionPerformOnDevice(sidewalkDevice: SidewalkDevice, action: Int) {
        when (action) {
            R.id.oobe_secure_channel, R.id.normal_secure_channel -> {
                val isRegister =
                    sidewalkDevice.beaconInfo.deviceMode == BeaconInfo.DeviceMode.Normal
                val bundle = bundleOf(
                    ConnectionViewModel.ARG_REGISTERED to isRegister,
                    ConnectionViewModel.ARG_SMSN to sidewalkDevice.truncatedSmsn
                )
                findNavController().navigate(
                    R.id.action_dashBoardFragment_to_connectionViewFragment, bundle
                )
            }
            R.id.oobe_register -> scanViewModel.registerDevice(sidewalkDevice.truncatedSmsn)
            R.id.coverage_test -> {
                val action = ScanFragmentDirections.actionDashBoardFragmentToCoverageTestStartFragment(sidewalkDevice.truncatedSmsn)
                findNavController().navigate(action)
            }
        }
    }

    private fun handleUiState(uiState: ScanUiState) {
        when (uiState) {
            is ScanUiState.Idle -> Unit
            is ScanUiState.Loading -> {
                when (val event = uiState.event) {
                    is ScanEvent.Scan -> {
                        view?.findViewById<TextView>(R.id.tv_scanning_status)?.text = "Scanning On"
                        unregisterDeviceAdapter.reset()
                        registerDeviceAdapter.reset()
                    }
                    is ScanEvent.Register -> {
                        progressDialog?.dismiss()
                        progressDialog = ProgressDialog(requireContext()).apply {
                            setMessage("Action ${event.smsn}")
                            setCancelable(false)
                            setButton(
                                DialogInterface.BUTTON_NEGATIVE,
                                getString(android.R.string.cancel)
                            ) { dialog, _ ->
                                dialog.dismiss()
                                scanViewModel.cancelRegisterDevice()
                            }
                        }
                        progressDialog?.show()
                    }
                }
            }

            is ScanUiState.Scanned -> {
                uiState.devices.forEach { device ->
                    val deviceMode = device.beaconInfo.deviceMode
                    if (deviceMode == BeaconInfo.DeviceMode.OOBE) {
                        unregisterDeviceAdapter.append(device)
                    } else if (deviceMode == BeaconInfo.DeviceMode.Normal) {
                        registerDeviceAdapter.append(device)
                    }
                }
            }
            is ScanUiState.Registered -> {
                progressDialog?.dismiss()
                when (uiState.registrationDetail) {
                    is RegistrationDetail.RegistrationSucceeded -> {
                        showMessage(
                            getString(R.string.register),
                            "Registration succeeded."
                        )
                    }
                    is RegistrationDetail.AlreadyRegistered -> {
                        showMessage(
                            getString(R.string.register),
                            "Already registered."
                        )
                    }
                }
                // Re-scan devices
                scanViewModel.scan(force = true)
            }
            is ScanUiState.Failure -> {
                progressDialog?.dismiss()
                val message = uiState.exception?.message ?: "null"
                when (uiState.event) {
                    is ScanEvent.Scan -> {
                        view?.findViewById<TextView>(R.id.tv_scanning_status)?.text = "Scanning Off"
                        showMessage("Scan failed", message)
                    }
                    is ScanEvent.Register -> showMessage("Register failed", message)
                }
            }
        }
    }
}
