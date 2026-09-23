package com.itkacher.data

import com.itkacher.Resources
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
    fun `body chunk crossing limit is replaced by limit message`() {
        val request = DebugRequest("1")

        request.addRequestBody("a".repeat(DebugRequest.MAX_BODY_LENGTH))
        request.addRequestBody("b")

        assertEquals(Resources.getString("max_length"), request.getRequestBodyString())
    }
}
