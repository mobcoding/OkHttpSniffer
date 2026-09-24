package io.nerdythings.okhttp.profiler.transfer

import org.junit.Assert.*
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class LogChunksTest {
    @Test fun `Chinese emoji and null are lossless within Android byte budget`() {
        val text = "{\"data\":\"" + "中文😀\u0000".repeat(20_000) + "\"}"
        val chunks = LogChunks.split(text).toList()
        assertEquals(text, chunks.joinToString(""))
        for (chunk in chunks) {
            val encoded = ByteArrayOutputStream()
            DataOutputStream(encoded).use { it.writeUTF(chunk) }
            assertTrue(encoded.size() - 2 <= LogChunks.MAX_BYTES)
            assertFalse(Character.isHighSurrogate(chunk.last()))
            assertFalse(Character.isLowSurrogate(chunk.first()))
        }
    }

    @Test fun `exact boundaries do not emit empty records`() {
        assertEquals(listOf("a".repeat(3000), "a".repeat(3000)), LogChunks.split("a".repeat(6000)).toList())
        assertTrue(LogChunks.split("").none())
    }

    @Test fun `response larger than ten MiB can be inspected without consuming app body`() {
        val json = "{\"data\":\"" + "x".repeat(11 * 1024 * 1024) + "\",\"end\":true}"
        val response = okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("https://example.test/").build())
            .protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
            .body(json.toResponseBody()).build()
        response.use {
            val captured = it.peekBody(Long.MAX_VALUE).use { body -> body.string() }
            assertEquals(json, LogChunks.split(captured).joinToString(""))
            assertEquals(json, it.body!!.string())
        }
    }
}
