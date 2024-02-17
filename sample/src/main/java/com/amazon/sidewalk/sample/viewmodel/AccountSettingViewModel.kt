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
import com.amazon.sidewalk.result.SidewalkResult
import com.amazon.sidewalk.sample.data.AccountSettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AccountSettingUiState {
    object Idle : AccountSettingUiState()
    class Loading(val event: AccountSettingEvent) : AccountSettingUiState()
    class LwaToken(val token: String) : AccountSettingUiState()
    object LoggedIn : AccountSettingUiState()
    object LoggedOut : AccountSettingUiState()
    class Deregistered(val smsn: String) : AccountSettingUiState()
    class Failure(val event: AccountSettingEvent, val exception: Throwable?) : AccountSettingUiState()
}

sealed class AccountSettingEvent(val message: String) {
    object RequestLwaToken : AccountSettingEvent("RequestLwaToken")
    object Login : AccountSettingEvent("Login")
    object Logout : AccountSettingEvent("Logout")
    class Deregister(val smsn: String) : AccountSettingEvent("Deregister")
}

@HiltViewModel
class AccountSettingViewModel @Inject constructor(
    private val accountSettingRepository: AccountSettingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountSettingUiState>(AccountSettingUiState.Idle)
    val uiState: StateFlow<AccountSettingUiState> = _uiState.asStateFlow()

    init {
        requestLwaToken()
    }

    private fun requestLwaToken(retry: Int = 3, delayMillis: Long = 200) {
        viewModelScope.launch {
            val result = accountSettingRepository.requestLwaToken()
            if (result is SidewalkResult.Failure && result.exception == null && retry > 0) {
                delay(delayMillis)
                requestLwaToken(retry - 1, delayMillis + 50)
                return@launch
            }

            val newUiState = when (result) {
                is SidewalkResult.Success -> AccountSettingUiState.LwaToken(result.value)
                is SidewalkResult.Failure -> AccountSettingUiState.Failure(
                    AccountSettingEvent.RequestLwaToken,
                    result.exception
                )
            }
            _uiState.update { newUiState }
        }
    }

    fun login() {
        viewModelScope.launch {
            val newUiState = when (val result = accountSettingRepository.login()) {
                is SidewalkResult.Success -> AccountSettingUiState.LoggedIn
                is SidewalkResult.Failure -> AccountSettingUiState.Failure(
                    AccountSettingEvent.Login,
                    result.exception
                )
            }
            _uiState.update { newUiState }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update {
                AccountSettingUiState.Loading(event = AccountSettingEvent.Logout)
            }
            val newUiState = when (val result = accountSettingRepository.logout()) {
                is SidewalkResult.Success -> AccountSettingUiState.LoggedOut
                is SidewalkResult.Failure -> AccountSettingUiState.Failure(
                    AccountSettingEvent.Logout,
                    result.exception
                )
            }
            _uiState.update { newUiState }
        }
    }

    fun deregisterDevice(smsn: String) {
        viewModelScope.launch {
            _uiState.update {
                AccountSettingUiState.Loading(event = AccountSettingEvent.Deregister(smsn))
            }
            val newUiState = when (
                val result = accountSettingRepository.deregisterDevice(smsn)
            ) {
                is SidewalkResult.Success -> AccountSettingUiState.Deregistered(smsn)
                is SidewalkResult.Failure -> AccountSettingUiState.Failure(
                    AccountSettingEvent.Deregister(smsn),
                    result.exception
                )
            }
            _uiState.update { newUiState }
        }
    }

    override fun onCleared() {
        accountSettingRepository.clear()
        super.onCleared()
    }
}
