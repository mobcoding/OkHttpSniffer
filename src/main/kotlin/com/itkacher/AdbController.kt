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
package com.itkacher

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.Client
import com.android.ddmlib.IDevice
import com.android.ddmlib.logcat.LogCatListener
import com.android.ddmlib.logcat.LogCatMessage
import com.android.ddmlib.logcat.LogCatReceiverTask
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.itkacher.data.DebugDevice
import com.itkacher.data.DebugProcess
import com.itkacher.data.MessageType
import com.itkacher.data.RequestDataSource
import com.itkacher.views.form.MainForm
import com.itkacher.views.PendingRequestUpdates
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.awt.event.ItemEvent
import java.util.concurrent.Executors
import javax.swing.DefaultComboBoxModel
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.Timer

class AdbController(val mainForm: MainForm, project: Project, val preferences: PluginPreferences) : Disposable {

    @Volatile
    private var selectedDevice: IDevice? = null
    @Volatile
    private var selectedProcess: DebugProcess? = null
    @Volatile
    private var captureGeneration = 0L
    private var logCatReceiverTask: LogCatReceiverTask? = null
    private var updatingDeviceList = false
    private var updatingProcessList = false
    @Volatile
    private var disposed = false
    private data class DeviceEntry(val item: DebugDevice, val online: Boolean, val lookup: SwingWorker<*, *>?)
    private val deviceEntries = mutableMapOf<String, DeviceEntry>()
    private val requestDataSource = RequestDataSource()

    val requestTableController = FormViewController(mainForm, project, requestDataSource)

    private val executor = Executors.newFixedThreadPool(1)
    private val logCatExecutor = Executors.newCachedThreadPool()
    private val pendingUpdates = PendingRequestUpdates()
    private val updateTimer = Timer(100) {
        pendingUpdates.drain(captureGeneration).forEach(requestTableController::insertOrUpdate)
    }

    private val deviceSelectionListener = java.awt.event.ItemListener { event ->
        if (event.stateChange == ItemEvent.SELECTED && !updatingDeviceList) {
            (event.item as? DebugDevice)?.let { debugDevice ->
                preferences.setSelectedDevice(debugDevice.serialNumber)
                attachToDevice(debugDevice.device)
            }
        }
    }

    private val processSelectionListener = java.awt.event.ItemListener { event ->
        if (event.stateChange == ItemEvent.SELECTED && !updatingProcessList) {
            (event.item as? DebugProcess)?.let { process ->
                captureGeneration++
                preferences.setSelectedProcessPackage(process.getClientKey())
                selectedProcess = process
                requestTableController.clear()
                log("selectedProcess $process")
            }
        }
    }

    private val deviceChangeListener = object : AndroidDebugBridge.IDeviceChangeListener {
        override fun deviceChanged(device: IDevice?, changeMask: Int) {
            updateDeviceList(AndroidDebugBridge.getBridge()?.devices)
        }

        override fun deviceConnected(device: IDevice?) {
            updateDeviceList(AndroidDebugBridge.getBridge()?.devices)
        }

        override fun deviceDisconnected(device: IDevice?) {
            updateDeviceList(AndroidDebugBridge.getBridge()?.devices)
        }
    }

    private val bridgeChangeListener = AndroidDebugBridge.IDebugBridgeChangeListener { bridge ->
        val devices = bridge?.devices
        if (devices?.isNotEmpty() == true) {
            log("addDebugBridgeChangeListener $bridge")
            updateDeviceList(devices)
        } else {
            log("addDebugBridgeChangeListener EMPTY $bridge and connected ${bridge?.isConnected}")
            updateDeviceList(devices)
        }
    }

    private val clientChangeListener = AndroidDebugBridge.IClientChangeListener { client, _ ->
        updateClient(client)
    }

    private val deviceListener = object : LogCatListener {
        override fun log(messages: List<LogCatMessage>) {
            val generation = captureGeneration
            val pid = selectedProcess?.pid ?: return
            val relevant = messages.filter { it.header.pid == pid && it.header.tag.startsWith(TAG_KEY) }
            if (relevant.isEmpty() || executor.isShutdown) return
            executor.execute {
                relevant.forEach { line ->
                    if (generation != captureGeneration) return@execute
                    val tag = line.header.tag
                    val selected = selectedProcess
                    if (selected != null && selected.pid == line.header.pid && tag.startsWith(TAG_KEY)) {
                        val sequences = tag.split(TAG_DELIMITER)
                        if (sequences.size == 3) {
                            val id = sequences[1]
                            val messageType = MessageType.fromString(sequences[2])
                            val debugRequest = requestDataSource.getRequestFromMessage(id, messageType, line.message)
                            if (debugRequest != null) {
                                pendingUpdates.offer(generation, debugRequest)
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        updateTimer.start()
        mainForm.clearButton.addActionListener {
            captureGeneration++
            pendingUpdates.clear()
        }
        mainForm.deviceList.addItemListener(deviceSelectionListener)
        mainForm.appList.addItemListener(processSelectionListener)
        initDeviceList(project)
    }

    private fun initDeviceList(project: Project) {
        AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener)
        AndroidDebugBridge.addDebugBridgeChangeListener(bridgeChangeListener)
        AndroidDebugBridge.addClientChangeListener(clientChangeListener)
        val bridge0: AndroidDebugBridge? = AndroidSdkUtils.getDebugBridge(project)
        log("initDeviceList bridge0 ${bridge0?.isConnected}")
        updateDeviceList(bridge0?.devices)
    }

    private fun updateClient(client: Client?) {
        if (disposed) return
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { updateClient(client) }
            return
        }
        val prefSelectedPackage = preferences.getSelectedProcessPackage()
        val clientData = client?.clientData
        val clientModel = mainForm.appList.model
        if (clientData != null && clientModel != null) {
            for (i in 0 until clientModel.size) {
                val model = clientModel.getElementAt(i)
                if (model.pid == clientData.pid) {
                    log("updateClient ${clientData.pid}")
                    model.packageName = clientData.packageName
                    model.clientDescription = clientData.processName
                    if (model.getClientKey() == prefSelectedPackage) {
                        mainForm.appList.selectedItem = model
                        selectedProcess = model
                    }
                    break
                }
            }
            mainForm.appList.revalidate()
            mainForm.appList.repaint()
        }
    }

    private fun updateDeviceList(devices: Array<IDevice>?) {
        if (disposed) return
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { updateDeviceList(devices) }
            return
        }
        val connected = devices.orEmpty().associateBy { it.serialNumber }
        val online = connected.mapValues { (_, device) -> device.isOnline }
        deviceEntries.entries.removeIf { (serial, entry) ->
            if (connected[serial] === entry.item.device && online[serial] == entry.online) return@removeIf false
            entry.lookup?.cancel(true)
            true
        }
        val items = connected.map { (serial, device) ->
            deviceEntries.getOrPut(serial) {
                val item = DebugDevice(device)
                val isOnline = online.getValue(serial)
                val lookup = if (isOnline) item.loadDetails { name ->
                    if (!disposed && deviceEntries[serial]?.item === item) {
                        mainForm.deviceList.revalidate()
                        mainForm.deviceList.repaint()
                        // Migrate device names saved by older plugin versions.
                        val preference = preferences.getSelectedDevice()
                        if (name != serial && preference == name && name !in deviceEntries) {
                            preferences.setSelectedDevice(serial)
                            mainForm.deviceList.selectedItem = item
                        }
                    }
                } else null
                DeviceEntry(item, isOnline, lookup)
            }.item
        }
        val list = mainForm.deviceList
        val current = (list.selectedItem as? DebugDevice)?.serialNumber
        val selected = deviceEntries[preferences.getSelectedDevice()]?.item
            ?: deviceEntries[current]?.item ?: items.firstOrNull()
        if (list.itemCount != items.size || items.indices.any { list.getItemAt(it) !== items[it] } ||
            list.selectedItem !== selected) {
            updatingDeviceList = true
            try {
                list.model = DefaultComboBoxModel(items.toTypedArray())
                list.selectedItem = selected
            } finally {
                updatingDeviceList = false
            }
        }
        if (selected != null) {
            mainForm.mainContainer.isVisible = true
            attachToDevice(selected.device)
        } else {
            if (selectedDevice != null) captureGeneration++
            stopLogCatReceiver()
            selectedDevice = null
            selectedProcess = null
            requestTableController.clear()
            mainForm.mainContainer.isVisible = false
        }
    }

    private fun attachToDevice(device: IDevice) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { attachToDevice(device) }
            return
        }
        val deviceChanged = selectedDevice !== device
        if (deviceChanged) {
            captureGeneration++
            requestTableController.clear()
        }
        createProcessList(device)
        setListener(device)
    }

    private fun createProcessList(device: IDevice) {
        val prefSelectedPackage = preferences.getSelectedProcessPackage()
        var defaultSelection: DebugProcess? = null
        val debugProcessList = ArrayList<DebugProcess>()
        log("createProcessList ${device.clients.size}")
        for (client in device.clients) {
            val clientData = client.clientData
            val process = DebugProcess(
                    clientData.pid,
                    clientData.packageName,
                    clientData.processName
            )
            if (prefSelectedPackage == process.getClientKey()) {
                defaultSelection = process
            }
            log("addClient $process")
            debugProcessList.add(process)
        }
        val model = DefaultComboBoxModel<DebugProcess>(debugProcessList.toTypedArray())
        updatingProcessList = true
        try {
            mainForm.appList.model = model
            mainForm.appList.selectedItem = defaultSelection ?: debugProcessList.firstOrNull()
        } finally {
            updatingProcessList = false
        }
        selectedProcess = defaultSelection ?: debugProcessList.firstOrNull()
    }


    private fun log(text: String) {
        println(text)
    }

    private fun setListener(device: IDevice) {
        if (selectedDevice === device && logCatReceiverTask != null) {
            return
        }
        stopLogCatReceiver()
        val receiverTask = LogCatReceiverTask(device)
        receiverTask.addLogCatListener(deviceListener)
        logCatReceiverTask = receiverTask
        logCatExecutor.execute(receiverTask)
        selectedDevice = device
        val clients = device.clients
        if (clients != null) {
            for (client in clients) {
                updateClient(client)
            }
        }
    }

    override fun dispose() {
        disposed = true
        deviceEntries.values.forEach { it.lookup?.cancel(true) }
        deviceEntries.clear()
        updateTimer.stop()
        pendingUpdates.clear()
        captureGeneration++
        mainForm.deviceList.removeItemListener(deviceSelectionListener)
        mainForm.appList.removeItemListener(processSelectionListener)
        AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener)
        AndroidDebugBridge.removeDebugBridgeChangeListener(bridgeChangeListener)
        AndroidDebugBridge.removeClientChangeListener(clientChangeListener)
        stopLogCatReceiver()
        executor.shutdownNow()
        logCatExecutor.shutdownNow()
        requestTableController.dispose()
        requestDataSource.clear()
    }

    private fun stopLogCatReceiver() {
        logCatReceiverTask?.let { task ->
            task.removeLogCatListener(deviceListener)
            task.stop()
        }
        logCatReceiverTask = null
    }

    companion object {
        private const val TAG_KEY = "OKPRFL"
        private const val TAG_DELIMITER = "_"
        const val STRING_BUNDLE = "strings"
    }
}
