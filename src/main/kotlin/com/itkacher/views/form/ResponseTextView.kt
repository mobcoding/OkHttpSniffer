package com.itkacher.views.form

import com.itkacher.util.SystemUtil
import com.itkacher.views.FrameScrollPanel
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

/** All methods on this view run on the EDT; content is prepared by the render worker. */
class ResponseTextView {
    internal val editor = JTextArea().apply { isEditable = false }
    val panel = JPanel(BorderLayout())
    private val previous = JButton("Previous")
    private val next = JButton("Next")
    private val position = JLabel()
    private val navigation = JPanel(FlowLayout(FlowLayout.LEADING, 6, 0))
    private var content = responseTextDocument("")
    private var page = 0

    init {
        previous.addActionListener { showPage(page - 1) }
        next.addActionListener { showPage(page + 1) }
        navigation.add(previous)
        navigation.add(position)
        navigation.add(next)
        navigation.add(JButton("Copy all").apply {
            toolTipText = "Copy the complete text across all pages"
            addActionListener { SystemUtil.copyToClipBoard(content.text) }
        })
        panel.add(navigation, BorderLayout.NORTH)
        panel.add(FrameScrollPanel(editor), BorderLayout.CENTER)
        setContent(content)
    }

    fun setContent(value: ResponseTextContent) {
        content = value
        navigation.isVisible = value.pages.size > 1
        showPage(0)
    }

    internal fun showPage(index: Int) {
        page = index.coerceIn(0, content.pages.lastIndex)
        editor.document = content.pages[page]
        editor.caretPosition = 0
        previous.isEnabled = page > 0
        next.isEnabled = page < content.pages.lastIndex
        position.text = "Page ${page + 1} / ${content.pages.size} — complete text retained"
    }
}
