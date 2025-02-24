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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazon.sidewalk.SidewalkConnection
import com.amazon.sidewalk.message.SidewalkMessage
import com.amazon.sidewalk.result.RegistrationDetail
import com.amazon.sidewalk.result.SidewalkResult
import com.amazon.sidewalk.sample.data.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConnectionUiState {
    object Idle : ConnectionUiState()

    class Loading(
        val event: ConnectionEvent,
    ) : ConnectionUiState()

    class Connected(
        val connection: SidewalkConnection,
    ) : ConnectionUiState()

    class Registered(
        val registrationDetail: RegistrationDetail,
    ) : ConnectionUiState()

    class Written(
        val messages: List<Pair<SidewalkMessage, MessageType>>,
    ) : ConnectionUiState()

    class Read(
        val messages: List<Pair<SidewalkMessage, MessageType>>,
    ) : ConnectionUiState()

    object Disconnected : ConnectionUiState()

    class Failure(
        val event: ConnectionEvent,
        val exception: Throwable?,
    ) : ConnectionUiState()
}

sealed class ConnectionEvent(
    val message: String,
) {
    class Connect(
        smsn: String,
    ) : ConnectionEvent("Connect $smsn")

    object Register : ConnectionEvent("Register")

    class Write(
        message: SidewalkMessage,
    ) : ConnectionEvent("Write")

    object Subscribe : ConnectionEvent("Subscribe")

    object Disconnect : ConnectionEvent("Disconnect")
}

sealed class MessageType(
    val type: String,
) {
    object Read : MessageType("Read")

    object Write : MessageType("Write")
}

@HiltViewModel
class ConnectionViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val connectionRepository: ConnectionRepository,
    ) : ViewModel() {
        companion object {
            const val ARG_SMSN = "smsn"
            const val ARG_REGISTERED = "registered"
        }

        private val _uiState = MutableStateFlow<ConnectionUiState>(ConnectionUiState.Idle)
        val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

        private lateinit var sidewalkConnection: SidewalkConnection
        private val smsn: String by lazy {
            savedStateHandle.get<String>(ARG_SMSN)!!
        }

        private val subscribeList = mutableListOf<Pair<SidewalkMessage, MessageType>>()

        init {
            establishSecureConnect()
        }

        private fun establishSecureConnect() {
            viewModelScope.launch {
                _uiState.update {
                    ConnectionUiState.Loading(event = ConnectionEvent.Connect(smsn))
                }
                val newUiState =
                    when (
                        val result = connectionRepository.establishSecureConnect(smsn)
                    ) {
                        is SidewalkResult.Success -> {
                            sidewalkConnection = result.value
                            ConnectionUiState.Connected(result.value)
                        }
                        is SidewalkResult.Failure ->
                            ConnectionUiState.Failure(
                                ConnectionEvent.Connect(smsn),
                                result.exception,
                            )
                    }
                _uiState.update { newUiState }
            }
        }

        fun disconnectSecureChannel() {
            viewModelScope.launch {
                _uiState.update {
                    ConnectionUiState.Loading(event = ConnectionEvent.Disconnect)
                }
                val newUiState =
                    when (
                        val result = connectionRepository.disconnectSecureConnect(sidewalkConnection)
                    ) {
                        is SidewalkResult.Success -> {
                            subscribeList.clear()
                            ConnectionUiState.Disconnected
                        }
                        is SidewalkResult.Failure ->
                            ConnectionUiState.Failure(ConnectionEvent.Disconnect, result.exception)
                    }
                _uiState.update { newUiState }
            }
        }

        fun registerDevice() {
            viewModelScope.launch {
                _uiState.update {
                    ConnectionUiState.Loading(event = ConnectionEvent.Register)
                }
                val newUiState =
                    when (
                        val result = connectionRepository.registerDevice(sidewalkConnection)
                    ) {
                        is SidewalkResult.Success -> {
                            ConnectionUiState.Registered(registrationDetail = result.value)
                        }
                        is SidewalkResult.Failure ->
                            ConnectionUiState.Failure(
                                ConnectionEvent.Register,
                                result.exception,
                            )
                    }
                _uiState.update { newUiState }
            }
        }

        fun subscribe() {
            viewModelScope.launch {
                // Reset list first
                subscribeList.clear()

                connectionRepository
                    .subscribe(sidewalkConnection)
                    .collect { result ->
                        val newUiState =
                            when (result) {
                                is SidewalkResult.Success -> {
                                    subscribeList.add(Pair(result.value, MessageType.Read))
                                    ConnectionUiState.Read(subscribeList)
                                }
                                is SidewalkResult.Failure ->
                                    ConnectionUiState.Failure(ConnectionEvent.Subscribe, result.exception)
                            }
                        _uiState.update { newUiState }
                    }
            }
        }

        fun write(message: SidewalkMessage) {
            viewModelScope.launch {
                val newUiState =
                    when (
                        val result = connectionRepository.write(sidewalkConnection, message)
                    ) {
                        is SidewalkResult.Success -> {
                            subscribeList.add(Pair(message, MessageType.Write))
                            ConnectionUiState.Written(subscribeList)
                        }
                        is SidewalkResult.Failure ->
                            ConnectionUiState.Failure(
                                ConnectionEvent.Write(message),
                                result.exception,
                            )
                    }
                _uiState.update { newUiState }
            }
        }
    }
