package com.itkacher.data

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugRequestTest {
    @Test
    fun `http errors include status 400`() {
        val request = DebugRequest("1")

        request.responseCode = 400

        assertTrue(request.isFallenDown())
        request.responseCode = 399
        assertFalse(request.isFallenDown())
    }

    @Test
    fun `raw response does not contain request method or url`() {
        val request = DebugRequest("1").apply {
            method = "GET"
            url = "https://example.test/path"
            responseCode = 204
            addResponseHeader("Content-Type: text/plain")
            addResponseBody("done")
        }

        assertEquals("204\r\n\r\nContent-Type: text/plain\r\n\r\ndone", request.getRawResponse())
    }

    @Test
    fun `large JSON body survives chunk assembly and remains parseable`() {
        val request = DebugRequest("1")
        val value = "中文响应😀".repeat(100_000)
        val json = "{\"data\":\"$value\",\"end\":true}"
        json.chunked(1000).forEach {
            request.addRequestBody(it)
            request.addResponseBody(it)
        }
        assertEquals(json, request.getRequestBodyString())
        assertEquals(json, request.getResponseBodyString())
        val parsed = ObjectMapper().readTree(request.getResponseBodyString())
        assertEquals(value, parsed["data"].asText())
        assertTrue(parsed["end"].asBoolean())
    }
}
