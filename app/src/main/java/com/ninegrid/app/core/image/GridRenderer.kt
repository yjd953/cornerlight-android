package com.ninegrid.app.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.compose.ui.graphics.toArgb
import com.ninegrid.app.core.model.EditorSettings
import com.ninegrid.app.core.model.TilePreview
import kotlin.math.roundToInt

/** Stateless tile renderer; never allocates a full multi-tile composition bitmap. */
class GridRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
    }

    /** Renders small UI previews in publication order (left-to-right, then top-to-bottom). */
    fun renderPreviews(
        source: Bitmap,
        settings: EditorSettings,
        tileSize: Int = PREVIEW_TILE_SIZE,
    ): List<TilePreview> = buildList(settings.gridSpec.tileCount) {
        repeat(settings.gridSpec.rows) { row ->
            repeat(settings.gridSpec.columns) { column ->
                val index = row * settings.gridSpec.columns + column
                add(
                    TilePreview(
                        index = index,
                        row = row,
                        column = column,
                        bitmap = renderTile(source, settings, row, column, tileSize),
                    ),
                )
            }
        }
    }

    /** Renders one square tile so batch exports keep a bounded memory footprint. */
    fun renderTile(
        source: Bitmap,
        settings: EditorSettings,
        row: Int,
        column: Int,
        tileSize: Int = EXPORT_TILE_SIZE,
    ): Bitmap {
        require(!source.isRecycled) { "Source bitmap has already been recycled" }
        require(row in 0 until settings.gridSpec.rows)
        require(column in 0 until settings.gridSpec.columns)

        val result = createBitmap(tileSize, tileSize)
        val canvas = Canvas(result)
        val background = settings.backgroundColor.toArgb()
        canvas.drawColor(background)

        val inset = (tileSize * settings.paddingPercent / 100f)
            .roundToInt()
            .coerceIn(0, tileSize / 4)
        val inner = RectF(
            inset.toFloat(),
            inset.toFloat(),
            (tileSize - inset).toFloat(),
            (tileSize - inset).toFloat(),
        )
        if (inner.width() <= 0f || inner.height() <= 0f) return result

        val compositionWidth = tileSize * settings.gridSpec.columns
        val compositionHeight = tileSize * settings.gridSpec.rows
        val compositionRect = GridGeometry.sourceDrawRect(
            sourceWidth = source.width,
            sourceHeight = source.height,
            compositionWidth = compositionWidth,
            compositionHeight = compositionHeight,
            cropMode = settings.cropMode,
            focusX = settings.focusX,
            focusY = settings.focusY,
        )
        val tileRect = GridGeometry.tileDrawRect(
            compositionRect = compositionRect,
            row = row,
            column = column,
            tileSize = tileSize,
            inset = inset,
        )

        canvas.withClip(inner) {
            drawBitmap(
                source,
                null,
                RectF(tileRect.left, tileRect.top, tileRect.right, tileRect.bottom),
                paint,
            )
        }
        return result
    }

    companion object {
        const val PREVIEW_TILE_SIZE = 360
        const val EXPORT_TILE_SIZE = 1080
    }
}
