package com.ninegrid.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GridSpecTest {
    @Test
    fun `all supported layouts expose a consistent tile count`() {
        assertEquals(4, GridSpec.FOUR.tileCount)
        assertEquals(9, GridSpec.NINE.tileCount)
        assertEquals(12, GridSpec.TWELVE.tileCount)
        GridSpec.entries.forEach { spec ->
            assertEquals(spec.rows * spec.columns, spec.tileCount)
        }
    }
}
