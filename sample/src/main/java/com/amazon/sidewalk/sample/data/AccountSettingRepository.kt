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

package com.amazon.sidewalk.sample.data

import android.content.Context
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.AuthCancellation
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult
import com.amazon.identity.auth.device.api.authorization.ScopeFactory
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.amazon.sidewalk.Sidewalk
import com.amazon.sidewalk.authentication.SidewalkAuthProvider
import com.amazon.sidewalk.result.SidewalkResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AccountSettingRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val requestContext: RequestContext,
        private val sidewalk: Sidewalk,
        private val authProvider: SidewalkAuthProvider,
        private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun requestLwaToken(): SidewalkResult<String> =
            withContext(ioDispatcher) {
                authProvider.getToken()
            }

        private val authorizeListener by lazy {
            object : AuthorizeListener() {
                override fun onSuccess(result: AuthorizeResult) {
                    loginContinuation?.resume(SidewalkResult.Success(Unit))
                    unregisterListener()
                }

                override fun onError(error: AuthError) {
                    loginContinuation?.resume(SidewalkResult.Failure(error))
                    unregisterListener()
                }

                override fun onCancel(cancellation: AuthCancellation?) {
                    val description = cancellation?.description
                    loginContinuation?.resume(SidewalkResult.Failure(CancellationException(description)))
                    unregisterListener()
                }
            }
        }

        private var loginContinuation: Continuation<SidewalkResult<Unit>>? = null

        suspend fun login(): SidewalkResult<Unit> =
            suspendCoroutine { cont ->
                loginContinuation = cont
                requestContext.registerListener(authorizeListener)
                AuthorizationManager.authorize(
                    AuthorizeRequest
                        .Builder(requestContext)
                        .addScopes(ScopeFactory.scopeNamed("sidewalk::manage_endpoint"))
                        .shouldReturnUserData(false)
                        .build(),
                )
            }

        suspend fun logout(): SidewalkResult<Unit> =
            withContext(ioDispatcher) {
                // Clear SDK internal cache
                sidewalk.clearAccountCache()

                suspendCoroutine { cont ->
                    val listener =
                        object : Listener<Void?, AuthError> {
                            override fun onSuccess(response: Void?) {
                                cont.resume(SidewalkResult.Success(Unit))
                            }

                            override fun onError(error: AuthError) {
                                cont.resume(SidewalkResult.Failure(error))
                            }
                        }
                    AuthorizationManager.signOut(context, listener)
                }
            }

        suspend fun deregisterDevice(smsn: String): SidewalkResult<Unit> =
            withContext(ioDispatcher) {
                sidewalk.deregisterDevice(smsn, true)
            }

        fun clear() {
            unregisterListener()
        }

        private fun unregisterListener() {
            loginContinuation = null
            requestContext.unregisterListener(authorizeListener)
        }
    }
