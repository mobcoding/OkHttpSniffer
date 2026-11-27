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
package com.itkacher.data

import com.android.ddmlib.IDevice
import com.itkacher.extension.safe
import java.util.concurrent.ExecutionException
import javax.swing.SwingWorker

class DebugDevice(val device: IDevice) {
    val serialNumber: String = device.serialNumber
    private var label = serialNumber

    override fun toString(): String = label

    internal fun loadDetails(onLoaded: (String) -> Unit): SwingWorker<Pair<String?, String>, Unit> =
        object : SwingWorker<Pair<String?, String>, Unit>() {
            override fun doInBackground(): Pair<String?, String> {
                val name = device.name
                return name to formatLabel(name)
            }

            override fun done() {
                if (isCancelled) return
                val (name, resolvedLabel) = try {
                    get()
                } catch (_: ExecutionException) {
                    return // Keep the serial number when device metadata is unavailable.
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
                label = resolvedLabel
                onLoaded(name ?: serialNumber)
            }
        }.apply { execute() }

    private fun formatLabel(name: String?): String {
        return if(device.isEmulator) {
            val emulatorName = device.getProperty(IDevice.PROP_DEVICE_MODEL).safe().replace("_", " ")
            "Emulator $emulatorName Android ${device.getProperty(IDevice.PROP_BUILD_VERSION).safe()}, API ${device.getProperty(IDevice.PROP_BUILD_API_LEVEL).safe()}"
        } else {
            val deviceName = name?.takeIf { it.isNotBlank() }?.replace("_", " ") ?: serialNumber
            val shortened = if(deviceName.length > 20) {
                deviceName.substring(0, 20)
            } else {
                deviceName
            }
            "${shortened.replaceFirstChar { it.uppercase() }} Android ${device.getProperty(IDevice.PROP_BUILD_VERSION).safe()}, API ${device.getProperty(IDevice.PROP_BUILD_API_LEVEL).safe()}"
        }
    }
}
