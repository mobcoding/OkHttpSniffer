package com.itkacher.data.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.itkacher.data.DebugRequest
import com.itkacher.data.generation.printer.JavaModelPrinter
import com.itkacher.data.generation.printer.KotlinModelPrinter
import com.itkacher.views.json.JsonMutableTreeNode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationTest {
    @Test
    fun `curl escapes shell single quotes`() {
        val request = DebugRequest("1").apply {
            method = "POST"
            url = "https://example.test/o'clock"
            addRequestHeader("X-Name: O'Brien")
            addRequestBody("it's fine")
        }

        val curl = CurlRequest(request).toString()

        assertTrue(curl.contains("O'\\''Brien"))
        assertTrue(curl.contains("it'\\''s fine"))
        assertTrue(curl.contains("o'\\''clock"))
    }

    @Test
    fun `generated identifiers are valid and nested class names match`() {
        val json = ObjectMapper().readTree(
            """{"user-info":{"first-name":"Ada"},"1st value":1,"a.b":true,"class":"reserved"}"""
        )
        val classes = NodeToClassesConverter()
            .buildClasses(JsonMutableTreeNode("root-model", json))
            .getClasses()

        val kotlin = KotlinModelPrinter(classes).build().toString()
        val java = JavaModelPrinter(classes).build().toString()

        assertTrue(kotlin.contains("data class RootModel"))
        assertTrue(kotlin.contains("val userInfo: UserInfo"))
        assertTrue(kotlin.contains("val _1stValue: Int"))
        assertTrue(kotlin.contains("val aB: Boolean"))
        assertTrue(kotlin.contains("val class0: String"))
        assertTrue(kotlin.contains("data class UserInfo"))
        assertTrue(java.contains("private UserInfo userInfo;"))
        assertFalse(java.contains("android.support.annotation"))
    }
}
