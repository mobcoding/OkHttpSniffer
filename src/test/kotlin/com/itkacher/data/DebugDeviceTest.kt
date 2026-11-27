package com.itkacher.data

import com.android.ddmlib.IDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import javax.swing.SwingUtilities
import javax.swing.SwingWorker

class DebugDeviceTest {
    @Test
    fun `slow ADB lookup leaves the EDT free to render the serial number`() {
        val lookupStarted = CountDownLatch(1)
        val releaseLookup = CountDownLatch(1)
        val nameApplied = CountDownLatch(1)
        val device = Proxy.newProxyInstance(IDevice::class.java.classLoader, arrayOf(IDevice::class.java)) { _, method, args ->
            when (method.name) {
                "getSerialNumber" -> "test-serial"
                "getName" -> {
                    assertFalse(SwingUtilities.isEventDispatchThread())
                    lookupStarted.countDown()
                    assertTrue(releaseLookup.await(10, SECONDS))
                    "Pixel_6"
                }
                "isEmulator" -> false
                "getProperty" -> {
                    assertFalse(SwingUtilities.isEventDispatchThread())
                    when (args[0]) {
                        IDevice.PROP_BUILD_VERSION -> "16"
                        IDevice.PROP_BUILD_API_LEVEL -> "36"
                        else -> error("Unexpected property ${args[0]}")
                    }
                }
                else -> error("Unexpected device call ${method.name}")
            }
        } as IDevice
        lateinit var item: DebugDevice
        var worker: SwingWorker<*, *>? = null
        try {
            SwingUtilities.invokeAndWait {
                item = DebugDevice(device)
                worker = item.loadDetails { name ->
                    assertTrue(SwingUtilities.isEventDispatchThread())
                    assertEquals("Pixel_6", name)
                    nameApplied.countDown()
                }
            }
            assertTrue(lookupStarted.await(5, SECONDS))
            SwingUtilities.invokeAndWait { assertEquals("test-serial", item.toString()) }
            releaseLookup.countDown()
            assertTrue(nameApplied.await(5, SECONDS))
            SwingUtilities.invokeAndWait { assertEquals("Pixel 6 Android 16, API 36", item.toString()) }
        } finally {
            releaseLookup.countDown()
            worker?.cancel(true)
        }
    }
}
