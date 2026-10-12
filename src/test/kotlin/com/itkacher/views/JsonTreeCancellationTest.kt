package com.itkacher.views

import com.fasterxml.jackson.databind.ObjectMapper
import com.itkacher.views.json.JsonTreeModel
import com.itkacher.views.json.JsonMutableTreeNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CancellationException

class JsonTreeCancellationTest {
    @Test
    fun `tree label is bounded but copied value stays complete`() {
        val value = ObjectMapper().nodeFactory.textNode("中文😀".repeat(10000))
        val node = JsonMutableTreeNode("body", value)
        assertTrue(node.toString().length < 600)
        assertEquals(value, node.value)
    }

    @Test
    fun `obsolete tree rendering can be interrupted`() {
        val json = ObjectMapper().readTree("{\"items\":[1,2,3]}")
        Thread.currentThread().interrupt()
        try {
            JsonTreeModel(json)
            fail("Cancelled renders must stop building trees")
        } catch (_: CancellationException) {
            // Expected: switching requests does not finish building an obsolete tree.
        } finally {
            Thread.interrupted()
        }
        assertFalse(Thread.currentThread().isInterrupted)
    }
}
