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

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

data class DebugRequest(val id: String) {
    @Volatile
    var url: String? = null
    @Volatile
    var method: String? = null
    val requestHeaders = CopyOnWriteArrayList<String>()
    private val requestBody = StringBuilder()
    @Volatile
    var duration: String? = null
    @Volatile
    var responseCode: Int? = null
    @Volatile
    var requestTime: String? = null
        set(value) {
            field = if (value != null) {
                try {
                    SimpleDateFormat("HH:mm:ss").format(Date(value.toLong()))
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    @Volatile
    var isClosed = false
    val responseHeaders = CopyOnWriteArrayList<String>()
    private val responseBody = StringBuilder()
    @Volatile
    var errorMessage: String? = null

    private val trash = StringBuilder()

    fun addRequestHeader(header: String) {
        requestHeaders.add(header)
    }

    @Synchronized
    fun addRequestBody(bodyPart: String) {
        requestBody.append(bodyPart)
    }

    fun addResponseHeader(header: String) {
        responseHeaders.add(header)
    }

    @Synchronized
    fun addResponseBody(bodyPart: String) {
        responseBody.append(bodyPart)
    }

    fun trash(message: String) {
        trash.append(message)
    }

    @Synchronized
    fun getRequestBodyString(): String {
        return requestBody.toString()
    }

    @Synchronized
    fun getResponseBodyString(): String {
        return responseBody.toString()
    }

    override fun toString(): String {
        return "$id $url $duration"
    }

    @Synchronized
    fun getRawRequest(): String? {
        return getRawDataString(requestHeaders, requestBody, "$method $url").toString()
    }

    @Synchronized
    fun getRawResponse(): String? {
        return getRawDataString(responseHeaders, responseBody, responseCode?.toString().orEmpty()).toString()
    }

    private fun getRawDataString(headers: List<String>, body: StringBuilder, startLine: String): StringBuilder {
        val builder = StringBuilder()
        builder.append(startLine)
                .append(NEW_LINE)
                .append(NEW_LINE)
        for (requestHeader in headers) {
            builder.append(requestHeader)
                    .append(NEW_LINE)
        }
        builder.append(NEW_LINE)
        builder.append(body)
        return builder
    }

    fun closeResponse() {
        isClosed = true
    }

    fun isFallenDown(): Boolean {
        val code = responseCode
        return (code != null && code >= 400) || (isClosed && errorMessage != null)
    }

    fun isValid(): Boolean {
        return method != null
    }

    companion object {
        const val SPACE = " "
        const val NEW_LINE = "\r\n"
    }
}
