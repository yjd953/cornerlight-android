package com.ninegrid.app.core.image

import com.ninegrid.app.core.model.CropMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GridGeometryTest {
    @Test
    fun `cover fills a square composition and centers by default`() {
        val rect = GridGeometry.sourceDrawRect(
            sourceWidth = 2000,
            sourceHeight = 1000,
            compositionWidth = 900,
            compositionHeight = 900,
            cropMode = CropMode.COVER,
            focusX = 0.5f,
            focusY = 0.5f,
        )

        assertEquals(-450f, rect.left, 0.01f)
        assertEquals(0f, rect.top, 0.01f)
        assertEquals(1800f, rect.width, 0.01f)
        assertEquals(900f, rect.height, 0.01f)
    }

    @Test
    fun `contain keeps the complete source visible`() {
        val rect = GridGeometry.sourceDrawRect(
            sourceWidth = 2000,
            sourceHeight = 1000,
            compositionWidth = 900,
            compositionHeight = 900,
            cropMode = CropMode.CONTAIN,
            focusX = 0.5f,
            focusY = 0.5f,
        )

        assertEquals(0f, rect.left, 0.01f)
        assertEquals(225f, rect.top, 0.01f)
        assertEquals(900f, rect.width, 0.01f)
        assertEquals(450f, rect.height, 0.01f)
    }

    @Test
    fun `focus moves cover crop to the selected edge`() {
        val left = GridGeometry.sourceDrawRect(2000, 1000, 900, 900, CropMode.COVER, 0f, 0.5f)
        val right = GridGeometry.sourceDrawRect(2000, 1000, 900, 900, CropMode.COVER, 1f, 0.5f)

        assertEquals(0f, left.left, 0.01f)
        assertEquals(-900f, right.left, 0.01f)
    }

    @Test
    fun `tile mapping preserves padding and global position`() {
        val composition = FloatRect(0f, 0f, 1080f, 1080f)
        val centerTile = GridGeometry.tileDrawRect(
            compositionRect = composition,
            row = 1,
            column = 1,
            tileSize = 360,
            inset = 18,
        )

        assertEquals(-306f, centerTile.left, 0.01f)
        assertEquals(-306f, centerTile.top, 0.01f)
        assertEquals(666f, centerTile.right, 0.01f)
        assertEquals(666f, centerTile.bottom, 0.01f)
    }

    @Test
    fun `focus values are clamped before positioning`() {
        val underflow = GridGeometry.sourceDrawRect(2000, 1000, 900, 900, CropMode.COVER, -2f, 0.5f)
        val overflow = GridGeometry.sourceDrawRect(2000, 1000, 900, 900, CropMode.COVER, 3f, 0.5f)

        assertEquals(0f, underflow.left, 0.01f)
        assertEquals(-900f, overflow.left, 0.01f)
    }

    @Test
    fun `asymmetric twelve-grid composition uses four columns and three rows`() {
        val rect = GridGeometry.sourceDrawRect(4000, 3000, 1440, 1080, CropMode.COVER, 0.5f, 0.5f)

        assertEquals(0f, rect.left, 0.01f)
        assertEquals(0f, rect.top, 0.01f)
        assertEquals(1440f, rect.width, 0.01f)
        assertEquals(1080f, rect.height, 0.01f)
    }

    @Test
    fun `invalid geometry fails close to its caller`() {
        assertThrows(IllegalArgumentException::class.java) {
            GridGeometry.sourceDrawRect(0, 100, 300, 300, CropMode.COVER, 0.5f, 0.5f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GridGeometry.tileDrawRect(FloatRect(0f, 0f, 100f, 100f), 0, 0, 100, 50)
        }
    }
}
