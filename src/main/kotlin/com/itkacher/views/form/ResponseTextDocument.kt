package com.itkacher.views.form

import java.util.concurrent.CancellationException
import javax.swing.text.PlainDocument

data class ResponseTextContent(val text: String, val pages: List<PlainDocument>)

/** Bound each Swing layout, including pathological single-line Unicode responses. */
fun responseTextDocument(text: String?): ResponseTextContent {
    val value = text.orEmpty()
    val pages = ArrayList<PlainDocument>()
    var start = 0
    do {
        if (Thread.currentThread().isInterrupted) throw CancellationException()
        var end = minOf(start + 8192, value.length)
        if (end < value.length) {
            if (Character.isHighSurrogate(value[end - 1])) end--
            if (value[end - 1] == '\r' && value[end] == '\n') end--
        }
        pages.add(PlainDocument().apply { insertString(0, value.substring(start, end), null) })
        start = end
    } while (start < value.length)
    return ResponseTextContent(value, pages)
}
