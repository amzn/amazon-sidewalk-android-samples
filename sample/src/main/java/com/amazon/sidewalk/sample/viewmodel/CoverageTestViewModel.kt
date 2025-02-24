/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All rights reserved.
 *
 * AMAZON PROPRIETARY/CONFIDENTIAL
 *
 * You may not use this file except in compliance with the terms and
 * conditions set forth in the accompanying LICENSE.txt file.
 *
 * THESE MATERIALS ARE PROVIDED ON AN "AS IS" BASIS. AMAZON SPECIFICALLY
 * DISCLAIMS, WITH RESPECT TO THESE MATERIALS, ALL WARRANTIES, EXPRESS,
 * IMPLIED, OR STATUTORY, INCLUDING THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
 */

package com.amazon.sidewalk.sample.viewmodel

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazon.sidewalk.SidewalkConnection
import com.amazon.sidewalk.coverage.SidewalkCoverageTestEvent
import com.amazon.sidewalk.coverage.SidewalkCoverageTestReport
import com.amazon.sidewalk.result.SidewalkResult
import com.amazon.sidewalk.sample.data.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CoverageTestUiState {
    object Idle : CoverageTestUiState()

    class Loading(
        val event: CoverageTestEvent,
    ) : CoverageTestUiState()

    class TestProgress(
        val eventTimeMs: Long,
        val maxTime: Long,
    ) : CoverageTestUiState()

    object InProgress : CoverageTestUiState()

    class ReportFetched(
        val data: SidewalkCoverageTestReport,
    ) : CoverageTestUiState()

    object Connected : CoverageTestUiState()

    class Disconnected(
        val event: CoverageTestEvent,
    ) : CoverageTestUiState()

    object TestCompleted : CoverageTestUiState()

    class Failure(
        val event: CoverageTestEvent,
        val exception: Throwable?,
    ) : CoverageTestUiState()
}

sealed class CoverageTestEvent {
    object Start : CoverageTestEvent()

    object Stop : CoverageTestEvent()

    object Report : CoverageTestEvent()

    class Connect(
        smsn: String,
    ) : CoverageTestEvent()

    object Disconnect : CoverageTestEvent()

    object Subscribe : CoverageTestEvent()
}

@HiltViewModel
class CoverageTestViewModel
    @Inject
    constructor(
        private val connectionRepository: ConnectionRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow<CoverageTestUiState>(CoverageTestUiState.Idle)
        val uiState: StateFlow<CoverageTestUiState> = _uiState.asStateFlow()

        private var sidewalkConnection: SidewalkConnection? = null
        private var timer: CountDownTimer? = null
        private var totalTestDuration: Long = 0

        fun startCoverageTest(
            pingInterval: Int,
            testDuration: Int,
            progressMode: Boolean,
        ) {
            viewModelScope.launch {
                sidewalkConnection?.let { connection ->
                    _uiState.update {
                        CoverageTestUiState.Loading(event = CoverageTestEvent.Start)
                    }
                    connectionRepository
                        .startCoverageTest(
                            connection,
                            pingInterval,
                            testDuration,
                            progressMode,
                        ).collect { result ->
                            val newUiState =
                                when (result) {
                                    is SidewalkResult.Success -> {
                                        handleCoverageTestEvent(result.value)
                                    }
                                    is SidewalkResult.Failure -> {
                                        CoverageTestUiState.Failure(
                                            CoverageTestEvent.Start,
                                            result.exception,
                                        )
                                    }
                                }
                            _uiState.update { newUiState }
                        }
                }
            }
        }

        fun stopCoverageTest() {
            viewModelScope.launch {
                timer?.cancel()
                sidewalkConnection?.let { connection ->
                    _uiState.update {
                        CoverageTestUiState.Loading(event = CoverageTestEvent.Stop)
                    }
                    connectionRepository.stopCoverageTest(connection)
                    _uiState.update {
                        CoverageTestUiState.TestCompleted
                    }
                }
            }
        }

        private fun countDownTime(testDurationInSec: Int) {
            timer =
                object : CountDownTimer((testDurationInSec * 1000).toLong(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        _uiState.update {
                            CoverageTestUiState.TestProgress(millisUntilFinished, totalTestDuration)
                        }
                    }

                    override fun onFinish() {
                        _uiState.update {
                            CoverageTestUiState.TestCompleted
                        }
                    }
                }
            timer?.start()
        }

        private fun handleCoverageTestEvent(event: SidewalkCoverageTestEvent): CoverageTestUiState =
            when (event) {
                is SidewalkCoverageTestEvent.CollectingReport -> {
                    CoverageTestUiState.Loading(CoverageTestEvent.Report)
                }
                is SidewalkCoverageTestEvent.PingEvent -> {
                    Log.i(
                        "SidewalkCoverageTestEvent",
                        "PingTime: ${event.eventTimeMs}",
                    )
                    CoverageTestUiState.Idle
                }
                is SidewalkCoverageTestEvent.PongEvent -> {
                    Log.i("SidewalkCoverageTestEvent", "Pong Time: ${event.eventTimeMs}")
                    CoverageTestUiState.Idle
                }
                is SidewalkCoverageTestEvent.MissingPongEvent -> {
                    Log.i("SidewalkCoverageTestEvent", "Missed Pong Time: ${event.eventTimeMs}")
                    CoverageTestUiState.Idle
                }
                is SidewalkCoverageTestEvent.TestReportEvent -> {
                    CoverageTestUiState.ReportFetched(event.data)
                }
                is SidewalkCoverageTestEvent.TestStart -> {
                    totalTestDuration = (event.testDuration * 1000).toLong()
                    countDownTime(event.testDuration)
                    CoverageTestUiState.InProgress
                }
            }

        fun establishSecureConnect(smsn: String) {
            viewModelScope.launch {
                _uiState.update {
                    CoverageTestUiState.Loading(
                        CoverageTestEvent.Connect(smsn),
                    )
                }
                val newUiState =
                    when (
                        val result = connectionRepository.establishSecureConnect(smsn)
                    ) {
                        is SidewalkResult.Success -> {
                            sidewalkConnection = result.value
                            subscribe()
                            CoverageTestUiState.Connected
                        }
                        is SidewalkResult.Failure ->
                            CoverageTestUiState.Failure(
                                CoverageTestEvent.Connect(smsn),
                                result.exception,
                            )
                    }
                _uiState.update { newUiState }
            }
        }

        fun disconnectSecureConnection() {
            viewModelScope.launch {
                sidewalkConnection?.let { connection ->
                    _uiState.update {
                        CoverageTestUiState.Loading(
                            CoverageTestEvent.Disconnect,
                        )
                    }
                    val newUiState =
                        when (
                            val result = connectionRepository.disconnectSecureConnect(connection)
                        ) {
                            is SidewalkResult.Success -> {
                                CoverageTestUiState.Disconnected(CoverageTestEvent.Stop)
                            }
                            is SidewalkResult.Failure ->
                                CoverageTestUiState.Failure(
                                    CoverageTestEvent.Disconnect,
                                    result.exception,
                                )
                        }
                    _uiState.update { newUiState }
                }
            }
        }

        private fun subscribe() {
            viewModelScope.launch {
                sidewalkConnection?.let { connection ->
                    connectionRepository.subscribe(connection).collect { result ->
                        val newUiState =
                            when (result) {
                                is SidewalkResult.Success -> CoverageTestUiState.Idle
                                is SidewalkResult.Failure ->
                                    CoverageTestUiState.Failure(
                                        CoverageTestEvent.Subscribe,
                                        result.exception,
                                    )
                            }
                        _uiState.update { newUiState }
                    }
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            sidewalkConnection = null
        }
    }
