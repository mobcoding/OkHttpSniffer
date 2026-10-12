package com.itkacher.views.form

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

class ResponseTextDocumentTest {
    @Test
    fun `large unicode response is prepared off EDT and displayed without truncation`() {
        val body = "{\"data\":\"" + "中文😀".repeat(300_000) + "\"}"
        val worker = Executors.newSingleThreadExecutor()
        try {
            val documents = worker.submit<Pair<ResponseTextContent, ResponseTextContent>> {
                assertFalse(SwingUtilities.isEventDispatchThread())
                responseTextDocument(body) to responseTextDocument(body)
            }.get(20, TimeUnit.SECONDS)
            val document = documents.first
            assertEquals(body, document.text)
            assertEquals(body, document.pages.joinToString("") { it.getText(0, it.length) })
            assertTrue(document.pages.all { it.length <= 8192 })
            SwingUtilities.invokeAndWait {
                val view = ResponseTextView()
                view.setContent(document)
                assertEquals(document.pages.first().getText(0, document.pages.first().length), view.editor.text)
                view.showPage(document.pages.lastIndex)
                assertEquals(document.pages.last().getText(0, document.pages.last().length), view.editor.text)
                assertFalse(view.editor.lineWrap)
                assertEquals(0, view.editor.caretPosition)
                view.setContent(documents.second)
                view.setContent(responseTextDocument(""))
                assertEquals(body, document.text)
            }
        } finally {
            worker.shutdownNow()
        }
    }

    @Test
    fun `page boundaries preserve surrogate pairs and CRLF`() {
        listOf("x".repeat(8191) + "😀end", "x".repeat(8191) + "\r\nend", "").forEach { text ->
            val content = responseTextDocument(text)
            assertEquals(text, content.pages.joinToString("") { it.getText(0, it.length) })
            content.pages.dropLast(1).forEach {
                val last = it.getText(it.length - 1, 1)[0]
                assertFalse(Character.isHighSurrogate(last))
                assertNotEquals('\r', last)
            }
        }
    }
}
