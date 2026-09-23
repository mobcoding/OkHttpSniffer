/**
 * Copyright 2018 LocaleBro.com [Ievgenii Tkachenko(gektor650@gmail.com)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itkacher.data

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RequestDataSource {
    private val requestMapById = HashMap<String, DebugRequest>()
    private val lock = ReentrantLock()

    fun getRequestFromMessage(id: String, type: MessageType, message: String): DebugRequest? {
        if (type == MessageType.UNKNOWN) return null
        return lock.withLock {
            val request = requestMapById.getOrPut(id) { DebugRequest(id) }
            fillRequest(type, request, message)
            request
        }
    }

    private fun fillRequest(messageType: MessageType, request: DebugRequest, message: String) {
        when (messageType) {
            MessageType.REQUEST_URL -> request.url = message
            MessageType.REQUEST_METHOD -> request.method = message
            MessageType.REQUEST_TIME -> request.requestTime = message
            MessageType.REQUEST_BODY -> request.addRequestBody(message)
            MessageType.REQUEST_HEADER -> request.addRequestHeader(message)
            MessageType.REQUEST_END -> Unit
            MessageType.RESPONSE_TIME -> request.duration = message
            MessageType.RESPONSE_STATUS -> request.responseCode = message.toIntOrNull()
            MessageType.RESPONSE_HEADER -> request.addResponseHeader(message)
            MessageType.RESPONSE_BODY -> request.addResponseBody(message)
            MessageType.RESPONSE_ERROR -> request.errorMessage = message
            MessageType.RESPONSE_END -> request.closeResponse()
            else -> {
                request.trash(message)
            }
        }
    }

    fun clear() {
        lock.withLock {
            requestMapById.clear()
        }
    }
}
