package com.ninegrid.app.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFileNamesTest {
    @Test
    fun `tile names preserve publication order and coordinates`() {
        assertEquals("01_20260809_120000_1_1.jpg", ExportFileNames.tile("20260809_120000", 0, 0, 0))
        assertEquals("12_20260809_120000_3_4.jpg", ExportFileNames.tile("20260809_120000", 11, 2, 3))
    }

    @Test
    fun `session and zip names remain filesystem safe`() {
        val session = ExportFileNames.session(0L)

        assertTrue(session.matches(Regex("\\d{8}_\\d{6}")))
        assertEquals("cornerlight-$session.zip", ExportFileNames.zip(session))
    }

    @Test
    fun `tile names reject invalid input`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExportFileNames.tile("", 0, 0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExportFileNames.tile("session", -1, 0, 0)
        }
    }
}
