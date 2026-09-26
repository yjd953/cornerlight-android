package com.ninegrid.app.core.image

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ninegrid.app.core.model.CropMode
import com.ninegrid.app.core.model.EditorSettings
import com.ninegrid.app.core.model.GridSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GridRendererInstrumentedTest {
    private val renderer = GridRenderer()

    @Test
    fun rendererProducesEverySupportedGridWithoutACompositionBitmap() {
        val source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(40, 120, 220))
        }

        try {
            GridSpec.entries.forEach { spec ->
                val settings = EditorSettings(gridSpec = spec, cropMode = CropMode.COVER)
                val previews = renderer.renderPreviews(source, settings, tileSize = 96)
                try {
                    assertEquals(spec.tileCount, previews.size)
                    previews.forEach { tile ->
                        assertEquals(96, tile.bitmap.width)
                        assertEquals(96, tile.bitmap.height)
                        assertTrue(Color.alpha(tile.bitmap.getPixel(48, 48)) > 0)
                    }
                } finally {
                    previews.forEach { it.bitmap.recycle() }
                }
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun paddingUsesTheSelectedBackgroundColor() {
        val source = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val background = androidx.compose.ui.graphics.Color(0xFF123456)
        val settings = EditorSettings(
            gridSpec = GridSpec.NINE,
            paddingPercent = 10f,
            backgroundColor = background,
        )
        val tile = renderer.renderTile(source, settings, row = 0, column = 0, tileSize = 100)

        try {
            assertEquals(background.toArgb(), tile.getPixel(2, 2))
            assertEquals(Color.RED, tile.getPixel(50, 50))
        } finally {
            tile.recycle()
            source.recycle()
        }
    }
}
