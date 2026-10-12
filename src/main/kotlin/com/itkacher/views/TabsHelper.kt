/**
 * Copyright 2018 LocaleBro.com [Ievgenii Tkachenko(gektor650@gmail.com)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itkacher.views

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBTabbedPane
import com.itkacher.Resources
import com.itkacher.data.DebugRequest
import com.itkacher.views.form.HeaderForm
import com.itkacher.views.form.JsonPlainTextForm
import com.itkacher.views.form.JsonTreeForm
import com.itkacher.views.form.RawForm
import com.itkacher.views.form.responseTextDocument
import com.itkacher.views.json.JTreeItemMenuListener
import com.itkacher.views.json.JsonTreeModel
import com.itkacher.views.json.JTreeMenuMouseAdapter
import java.awt.Color
import java.awt.Dimension
import java.io.IOException
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JLabel
import javax.swing.SwingUtilities
import kotlin.collections.HashMap


class TabsHelper(private val tabbedPane: JBTabbedPane,
                 private val menuListener: JTreeItemMenuListener) {

    private val executor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>())
    private var pendingRender: Future<*>? = null
    private val objectMapper = ObjectMapper()

    @Volatile
    private var disposed = false

    @Volatile
    private var currentRequest: DebugRequest? = null
    private val renderGeneration = AtomicLong()
    private var renderingClosedRequest = false
    private var preservingSelection = false

    private val tabTitles = HashMap<Int, String>()

    private val requestHeadersFrom = HeaderForm()
    private val requestRawForm = RawForm()
    private val requestJsonTreeForm = JsonTreeForm()
    private val requestJsonFormattedForm = JsonPlainTextForm()


    private val responseHeadersFrom = HeaderForm()
    private val responseRawForm = RawForm()
    private val responseJsonTreeForm = JsonTreeForm()
    private val responseJsonFormattedForm = JsonPlainTextForm()

    private val errorRawForm = RawForm()

    private var indexRequestRawTab = 0
    private var indexRequestHeaderTab = 0
    private var indexRequestTreeTab = 0
    private var indexRequestFormattedTab = 0

    private var indexResponseRawTab = 0
    private var indexResponseHeaderTab = 0
    private var indexResponseTreeTab = 0
    private var indexResponseFormattedTab = 0

    private var indexErrorTab = 0

    private var defaultBackground = Color.GRAY

    fun initialize() {
        tabbedPane.minimumSize = Dimension(100, 50)

        indexRequestRawTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_REQUEST_RAW.resName), requestRawForm.panel)
        indexRequestHeaderTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_REQUEST_HEADERS.resName), requestHeadersFrom.panel)
        indexRequestTreeTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_REQUEST_JSON.resName), requestJsonTreeForm.treePanel)
        indexRequestFormattedTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_REQUEST_FORMATTED.resName), requestJsonFormattedForm.panel)
        requestJsonTreeForm.tree.addMouseListener(JTreeMenuMouseAdapter(menuListener))

        indexResponseRawTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_RESPONSE_RAW.resName), responseRawForm.panel)
        indexResponseHeaderTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_RESPONSE_HEADERS.resName), responseHeadersFrom.panel)
        indexResponseTreeTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_RESPONSE_JSON.resName), responseJsonTreeForm.treePanel)
        indexResponseFormattedTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_RESPONSE_FORMATTED.resName), responseJsonFormattedForm.panel)

        indexErrorTab = tabbedPane.tabCount
        tabbedPane.addTab(Resources.getString(Tabs.TAB_ERROR_MESSAGE.resName), errorRawForm.panel)

        responseJsonTreeForm.tree.addMouseListener(JTreeMenuMouseAdapter(menuListener))
        defaultBackground = tabbedPane.getForegroundAt(0)

        clearTabs()
    }

    private fun updateRequestHeaderTab(headers: List<String>) {
        val listModel = CollectionListModel<String>(headers)
        requestHeadersFrom.headersList.model = listModel
    }

    private fun updateResponseHeaderTab(headers: List<String>) {
        val listModel = CollectionListModel<String>(headers)
        responseHeadersFrom.headersList.model = listModel
    }

    private val acceptedContentTypes = listOf(
            "text/plain",
            "text/html",
            "application/json"
    )

    private val contentTypeHeader = "content-type:"

    private fun isAcceptedHeaders(headers: List<String>): Boolean {
        return headers.isEmpty() || headers.find {
            var res = false
            for (type in acceptedContentTypes) {
                val text = it.lowercase(Locale.ROOT)
                if (text.startsWith(contentTypeHeader) && text.contains(type)) {
                    res = true
                }
            }
            res
        } != null
    }

    fun fill(debugRequest: DebugRequest) {
        if (disposed) return
        if (currentRequest === debugRequest && renderingClosedRequest) return
        val selectedTab = tabbedPane.selectedIndex
        clearTabs(Resources.getString("processing"), preserveSelection = true)
        currentRequest = debugRequest
        renderingClosedRequest = debugRequest.isClosed
        val closed = renderingClosedRequest
        val generation = renderGeneration.incrementAndGet()
        pendingRender = executor.submit {
            fun obsolete() = disposed || generation != renderGeneration.get() || Thread.currentThread().isInterrupted
            if (obsolete()) return@submit
            val requestHeaders = debugRequest.requestHeaders.toList()
            val responseHeaders = debugRequest.responseHeaders.toList()
            val requestDocument = responseTextDocument(debugRequest.getRawRequest())
            val responseDocument = if (closed) responseTextDocument(debugRequest.getRawResponse()) else null
            val error = debugRequest.errorMessage
            val errorDocument = responseTextDocument(error)
            if (obsolete()) return@submit
            //Skip non text response/requests
            val isRequestIsText = isAcceptedHeaders(debugRequest.requestHeaders)
            val isResponseIsText = isAcceptedHeaders(debugRequest.responseHeaders)

            val requestJson = if (isRequestIsText) {
                debugRequest.getRequestBodyString()
            } else {
                null
            }
            val requestJsonPair = if (closed) getTreeModelPrettifyPair(requestJson) else null
            if (obsolete()) return@submit
            val responseBody = if (isResponseIsText) {
                debugRequest.getResponseBodyString()
            } else {
                null
            }

            val responseJsonPair = if (closed) getTreeModelPrettifyPair(responseBody) else null
            if (obsolete()) return@submit
            val requestPrettyDocument = requestJsonPair?.second?.let { responseTextDocument(it) }
            val responsePrettyDocument = responseJsonPair?.second?.let { responseTextDocument(it) }
            if (obsolete()) return@submit

            SwingUtilities.invokeLater {
                if (disposed || generation != renderGeneration.get() || currentRequest !== debugRequest) return@invokeLater

                requestRawForm.setDocument(requestDocument)
                enableTab(indexRequestRawTab)

                if (requestHeaders.isNotEmpty()) {
                    updateRequestHeaderTab(requestHeaders)
                    enableTab(indexRequestHeaderTab)
                } else {
                    disableTab(indexRequestHeaderTab)
                }
                if (closed) {
                    responseRawForm.setDocument(responseDocument!!)
                    enableTab(indexResponseRawTab)

                    if (responseHeaders.isNotEmpty()) {
                        updateResponseHeaderTab(responseHeaders)
                        enableTab(indexResponseHeaderTab)
                    } else {
                        disableTab(indexResponseHeaderTab)
                    }

                    if (requestJsonPair?.first != null) {
                        requestJsonTreeForm.tree.model = requestJsonPair.first
                        enableTab(indexRequestTreeTab)
                    } else {
                        disableTab(indexRequestTreeTab)
                    }

                    if (requestJsonPair?.second != null && requestJsonPair.second != "") {
                        requestJsonFormattedForm.setDocument(requestPrettyDocument!!)
                        enableTab(indexRequestFormattedTab)
                    } else {
                        disableTab(indexRequestFormattedTab)
                    }

                    if (responseJsonPair?.first != null) {
                        responseJsonTreeForm.tree.model = responseJsonPair.first
                        enableTab(indexResponseTreeTab)
                    } else {
                        disableTab(indexResponseTreeTab)
                    }

                    if (responseJsonPair?.second != null && responseJsonPair.second != "") {
                        responseJsonFormattedForm.setDocument(responsePrettyDocument!!)
                        enableTab(indexResponseFormattedTab)
                    } else {
                        disableTab(indexResponseFormattedTab)
                    }

                    if (error.isNullOrBlank()) {
                        disableTab(indexErrorTab)
                    } else {
                        enableTab(indexErrorTab)
                        errorRawForm.setDocument(errorDocument)
                    }
                }
                if (selectedTab >= 0 && tabbedPane.isEnabledAt(selectedTab)) {
                    tabbedPane.selectedIndex = selectedTab
                }
            }
        }
    }

    private fun enableTab(tabIndex: Int) {
        val title = tabTitles[tabIndex]
        if (title != null) {
            val label = tabbedPane.getTabComponentAt(tabIndex)
            if (label is JLabel) {
                label.text = title
            }
        }
        tabbedPane.setEnabledAt(tabIndex, true)
        tabbedPane.setForegroundAt(tabIndex, defaultBackground)
        tabbedPane.setToolTipTextAt(tabIndex, null)
    }

    private fun disableTab(tabIndex: Int) {
        val label = tabbedPane.getTabComponentAt(tabIndex)
        if (label is JLabel) {
            if (tabTitles[tabIndex] == null) {
                tabTitles[tabIndex] = label.text
            }
            label.text = ""
        }

        tabbedPane.setEnabledAt(tabIndex, false)
        tabbedPane.setForegroundAt(tabIndex, Color.GRAY)
        tabbedPane.setToolTipTextAt(tabIndex, Resources.getString("empty"))
        if (!preservingSelection && tabbedPane.selectedIndex == tabIndex) {
            tabbedPane.selectedIndex = 0
        }
    }

    private fun parseModel(jsonString: String): JsonNode? {
        val trimmed = jsonString.trim()
        return if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                objectMapper.readTree(trimmed)
            } catch (e: IOException) {
                null
            }
        } else {
            null
        }
    }

    private fun prettifyNode(jsonNode: JsonNode): String? {
        return try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getTreeModelPrettifyPair(requestJson: String?): Pair<JsonTreeModel, String?>? {
        if(requestJson != null) {
            val jsonNode = parseModel(requestJson)
            jsonNode?.let {
                val prettyJson = prettifyNode(it)
                if (Thread.currentThread().isInterrupted) throw CancellationException()
                val model = JsonTreeModel(jsonNode)
                return Pair(model, prettyJson)
            }
        }
        return null
    }

    fun clearTabs(processingText: String = "", preserveSelection: Boolean = false) {
        pendingRender?.cancel(true)
        executor.queue.clear()
        currentRequest = null
        renderGeneration.incrementAndGet()
        preservingSelection = preserveSelection
        disableTab(indexRequestRawTab)
        disableTab(indexRequestHeaderTab)
        disableTab(indexRequestTreeTab)
        disableTab(indexRequestFormattedTab)
        disableTab(indexResponseRawTab)
        disableTab(indexResponseHeaderTab)
        disableTab(indexResponseTreeTab)
        disableTab(indexResponseFormattedTab)
        disableTab(indexErrorTab)
        preservingSelection = false

        responseRawForm.setText(processingText)
        updateResponseHeaderTab(Collections.singletonList(processingText))
        requestJsonFormattedForm.setText(processingText)
        requestJsonTreeForm.tree.model = null

        requestRawForm.setText(processingText)
        updateRequestHeaderTab(Collections.singletonList(processingText))
        responseJsonFormattedForm.setText(processingText)
        responseJsonTreeForm.tree.model = null

        errorRawForm.setText(processingText)
    }

    fun dispose() {
        disposed = true
        currentRequest = null
        renderGeneration.incrementAndGet()
        executor.shutdownNow()
    }

}
