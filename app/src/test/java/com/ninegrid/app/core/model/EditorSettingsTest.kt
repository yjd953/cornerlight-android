package com.ninegrid.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EditorSettingsTest {
    @Test
    fun `defaults describe the primary nine-grid workflow`() {
        val settings = EditorSettings.Default

        assertEquals(GridSpec.NINE, settings.gridSpec)
        assertEquals(CropMode.COVER, settings.cropMode)
        assertEquals(90, settings.quality)
        assertEquals(0.5f, settings.focusX)
        assertEquals(0.5f, settings.focusY)
    }

    @Test
    fun `rejects values outside editor contracts`() {
        assertThrows(IllegalArgumentException::class.java) { EditorSettings(paddingPercent = 12.1f) }
        assertThrows(IllegalArgumentException::class.java) { EditorSettings(quality = 59) }
        assertThrows(IllegalArgumentException::class.java) { EditorSettings(focusX = -0.01f) }
        assertThrows(IllegalArgumentException::class.java) { EditorSettings(focusY = 1.01f) }
    }
}
