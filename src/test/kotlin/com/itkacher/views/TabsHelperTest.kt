package com.itkacher.views

import com.intellij.ui.components.JBTabbedPane
import com.itkacher.data.DebugRequest
import com.itkacher.views.json.JTreeItemMenuListener
import com.itkacher.views.json.JsonMutableTreeNode
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

class TabsHelperTest {
    @Test
    fun `rapid selection shows latest response and clear prevents stale render`() {
        lateinit var tabs: JBTabbedPane
        lateinit var helper: TabsHelper
        val listener = object : JTreeItemMenuListener {
            override fun copyToClipboard(node: JsonMutableTreeNode) {}
            override fun openInEditor(node: JsonMutableTreeNode) {}
            override fun createJavaModel(node: JsonMutableTreeNode) {}
            override fun createKotlinModel(node: JsonMutableTreeNode) {}
        }
        fun request(id: String, body: String) = DebugRequest(id).apply {
            method = "GET"
            addResponseBody(body)
            closeResponse()
        }
        fun text(): String {
            val panel = tabs.getComponentAt(4) as JPanel
            val scrollPanel = panel.getComponent(1) as JPanel
            return ((scrollPanel.getComponent(0) as JScrollPane).viewport.view as JTextArea).text
        }
        try {
            SwingUtilities.invokeAndWait {
                tabs = JBTabbedPane()
                helper = TabsHelper(tabs, listener)
                helper.initialize()
                repeat(30) { helper.fill(request("$it", "[" + "0,".repeat(10000) + "0]")) }
                helper.fill(request("latest", "{\"latest\":true}"))
            }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
            var rendered = false
            while (!rendered && System.nanoTime() < deadline) {
                SwingUtilities.invokeAndWait { rendered = text().endsWith("{\"latest\":true}") }
                if (!rendered) Thread.sleep(20)
            }
            assertTrue("Latest selection must render", rendered)
            SwingUtilities.invokeAndWait {
                helper.fill(request("stale", "[1,2,3]"))
                helper.clearTabs()
            }
            Thread.sleep(150)
            SwingUtilities.invokeAndWait { assertEquals("", text()) }
        } finally {
            SwingUtilities.invokeAndWait { helper.dispose() }
        }
    }
}
