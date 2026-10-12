package com.itkacher.views

import com.itkacher.data.DebugRequest
import org.junit.Assert.*
import org.junit.Test

class PendingRequestUpdatesTest {
    @Test
    fun `thousands of chunks coalesce without losing response data`() {
        val updates = PendingRequestUpdates()
        val requests = (0 until 20).map { DebugRequest("$it") }
        repeat(1000) {
            requests.forEach { request ->
                request.addResponseBody("中文😀")
                updates.offer(1, request)
            }
        }
        requests.forEach { it.closeResponse(); updates.offer(1, it) }
        val batch = updates.drain(1)
        assertEquals(requests, batch)
        batch.forEach {
            assertTrue(it.isClosed)
            assertEquals("中文😀".repeat(1000), it.getResponseBodyString())
        }
        assertTrue(updates.drain(1).isEmpty())
    }

    @Test
    fun `switching capture and clearing discard stale notifications with bounded batches`() {
        val updates = PendingRequestUpdates()
        repeat(150) { updates.offer(1, DebugRequest("$it")) }
        val fresh = DebugRequest("new")
        updates.offer(2, fresh)
        assertEquals(listOf(fresh), updates.drain(2))
        repeat(150) { updates.offer(2, DebugRequest("$it")) }
        assertEquals(100, updates.drain(2).size)
        assertEquals(50, updates.drain(2).size)
        updates.offer(2, fresh)
        updates.clear()
        assertTrue(updates.drain(2).isEmpty())
    }
}
