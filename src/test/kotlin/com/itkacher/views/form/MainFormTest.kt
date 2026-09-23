package com.itkacher.views.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.SwingUtilities

class MainFormTest {
    @Test
    fun `toolbar buttons use compact platform icons`() {
        SwingUtilities.invokeAndWait {
            val form = MainForm()
            val buttons = listOf(
                form.localizeButton,
                form.donateButton,
                form.scrollToBottomButton,
                form.clearButton
            )

            buttons.forEach { button ->
                assertToolbarButton(button)
            }
        }
    }

    private fun assertToolbarButton(button: JButton) {
        assertEquals(Dimension(26, 26), button.preferredSize)
        assertEquals(16, button.icon.iconWidth)
        assertEquals(16, button.icon.iconHeight)
        assertNull(button.text)
        assertTrue(button.toolTipText.isNotBlank())
        assertEquals(button.toolTipText, button.accessibleContext.accessibleName)
    }
}
