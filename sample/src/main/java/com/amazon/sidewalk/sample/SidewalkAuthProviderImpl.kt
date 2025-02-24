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

package com.amazon.sidewalk.sample

import android.content.Context
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult
import com.amazon.identity.auth.device.api.authorization.ScopeFactory
import com.amazon.sidewalk.authentication.SidewalkAuthProvider
import com.amazon.sidewalk.result.SidewalkResult
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SidewalkAuthProviderImpl
    @Inject
    constructor(
        private val context: Context,
        private val logger: Logger,
    ) : SidewalkAuthProvider {
        override suspend fun getToken(): SidewalkResult<String> = obtainLwaToken(context, logger)

        private suspend fun obtainLwaToken(
            context: Context,
            logger: Logger,
        ): SidewalkResult<String> =
            suspendCoroutine { continuation ->
                val scopes = arrayOf(ScopeFactory.scopeNamed("sidewalk::manage_endpoint"))
                AuthorizationManager.getToken(
                    context,
                    scopes,
                    object : Listener<AuthorizeResult, AuthError> {
                        override fun onSuccess(result: AuthorizeResult) {
                            result.accessToken?.let {
                                logger.log(Level.INFO, "We have the LWA token")
                                continuation.resume(SidewalkResult.Success(it))
                            } ?: run {
                                logger.log(Level.INFO, "No accessToken found")
                                continuation.resume(SidewalkResult.Failure())
                            }
                        }

                        override fun onError(ae: AuthError) {
                            logger.log(Level.SEVERE, "Error: the user is not signed in, e=$ae")
                            continuation.resume(SidewalkResult.Failure(ae))
                        }
                    },
                )
            }
    }
