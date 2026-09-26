package com.ninegrid.app.core.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageSamplingTest {
    @Test
    fun `keeps images already inside the decode boundary`() {
        assertEquals(1, ImageSampling.calculate(width = 4096, height = 1200, maxEdge = 4096))
    }

    @Test
    fun `uses the smallest power of two that fits the boundary`() {
        assertEquals(2, ImageSampling.calculate(width = 8000, height = 6000, maxEdge = 4096))
        assertEquals(4, ImageSampling.calculate(width = 16_385, height = 1000, maxEdge = 4096))
    }

    @Test
    fun `rejects invalid dimensions and limits`() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageSampling.calculate(width = 0, height = 100, maxEdge = 4096)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ImageSampling.calculate(width = 100, height = 100, maxEdge = 0)
        }
    }
}
