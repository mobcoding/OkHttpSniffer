package com.itkacher.data

import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestDataSourceTest {
    @Test
    fun `request end marker is not added as a header`() {
        val source = RequestDataSource()

        source.getRequestFromMessage("1", MessageType.REQUEST_METHOD, "GET")
        val request = source.getRequestFromMessage("1", MessageType.REQUEST_END, "request-finished")!!

        assertTrue(request.requestHeaders.isEmpty())
    }

    @Test
    fun `clear starts a fresh request cache`() {
        val source = RequestDataSource()
        val oldRequest = source.getRequestFromMessage("1", MessageType.REQUEST_METHOD, "GET")!!

        source.clear()
        val newRequest = source.getRequestFromMessage("1", MessageType.REQUEST_METHOD, "POST")!!

        assertNotSame(oldRequest, newRequest)
    }
}
