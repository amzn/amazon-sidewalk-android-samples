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

import com.amazon.sidewalk.Sidewalk
import com.amazon.sidewalk.SidewalkConnection
import com.amazon.sidewalk.device.SidewalkDeviceDescriptor
import com.amazon.sidewalk.message.SidewalkMessage
import com.amazon.sidewalk.result.RegisterResult
import com.amazon.sidewalk.result.SidewalkResult
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ConnectionRepository @Inject constructor(
    private val sidewalk: Sidewalk,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun establishSecureConnect(
        descriptor: SidewalkDeviceDescriptor
    ): SidewalkResult<SidewalkConnection> {
        return withContext(ioDispatcher) {
            sidewalk.secureConnect(descriptor)
        }
    }

    suspend fun disconnectSecureConnect(connection: SidewalkConnection): SidewalkResult<Unit> {
        return withContext(ioDispatcher) {
            connection.disconnect()
        }
    }

    fun register(connection: SidewalkConnection): Flow<RegisterResult> {
        return sidewalk.register(connection).flowOn(ioDispatcher)
    }

    fun subscribe(connection: SidewalkConnection): Flow<SidewalkResult<SidewalkMessage>> {
        return connection.subscribe().flowOn(ioDispatcher)
    }

    suspend fun write(
        connection: SidewalkConnection,
        message: SidewalkMessage
    ): SidewalkResult<Unit> {
        return withContext(ioDispatcher) {
            connection.write(message)
        }
    }
}
