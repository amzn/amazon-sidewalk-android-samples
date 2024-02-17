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

package com.amazon.sidewalk.sample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazon.sidewalk.device.SidewalkDevice
import com.amazon.sidewalk.result.RegistrationDetail
import com.amazon.sidewalk.result.SidewalkResult
import com.amazon.sidewalk.sample.data.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Idle : ScanUiState()
    class Loading(val event: ScanEvent) : ScanUiState()
    class Scanned(val devices: List<SidewalkDevice>) : ScanUiState()
    class Registered(val registrationDetail: RegistrationDetail) : ScanUiState()
    class Failure(val event: ScanEvent, val exception: Throwable?) : ScanUiState()
}

sealed class ScanEvent(val message: String) {
    object Scan : ScanEvent("Scan")
    class Register(val smsn: String) : ScanEvent("Register")
}

@HiltViewModel
class ScanViewModel @Inject constructor(private val scanRepository: ScanRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val scanList = mutableListOf<SidewalkDevice>()

    private var scanJob: Job? = null
    private var registerJob: Job? = null

    fun scan(force: Boolean = false) {
        if (force) {
            cancelScan()
        }
        scanJob = viewModelScope.launch {
            // Reset scanning list first
            scanList.clear()

            _uiState.update {
                ScanUiState.Loading(event = ScanEvent.Scan)
            }
            scanRepository.scan()
                .collect { result ->
                    val newUiState = when (result) {
                        is SidewalkResult.Success -> {
                            scanList.add(result.value)
                            ScanUiState.Scanned(scanList)
                        }
                        is SidewalkResult.Failure ->
                            ScanUiState.Failure(ScanEvent.Scan, result.exception)
                    }
                    _uiState.update { newUiState }
                }
        }
    }

    fun registerDevice(smsn: String) {
        registerJob = viewModelScope.launch {
            _uiState.update {
                ScanUiState.Loading(event = ScanEvent.Register(smsn))
            }
            val newUiState = when (
                val result = scanRepository.registerDevice(smsn)
            ) {
                is SidewalkResult.Success -> {
                    ScanUiState.Registered(registrationDetail = result.value)
                }
                is SidewalkResult.Failure ->
                    ScanUiState.Failure(ScanEvent.Register(smsn), result.exception)
            }
            _uiState.update { newUiState }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun cancelRegisterDevice() {
        registerJob?.cancel()
        registerJob = null
    }
}
