package com.ninegrid.app.core.image

import com.ninegrid.app.core.model.CropMode
import kotlin.math.max
import kotlin.math.min

/** Rectangle in renderer pixel coordinates. */
data class FloatRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** Geometry shared by the interactive preview and the final bitmap renderer. */
object GridGeometry {
    /**
     * Maps the source into the full multi-tile composition.
     *
     * [focusX] and [focusY] describe where any overflow is placed: `0` aligns start, `0.5`
     * centres, and `1` aligns end. Keeping this calculation pure prevents preview/export drift.
     */
    fun sourceDrawRect(
        sourceWidth: Int,
        sourceHeight: Int,
        compositionWidth: Int,
        compositionHeight: Int,
        cropMode: CropMode,
        focusX: Float,
        focusY: Float,
    ): FloatRect {
        require(sourceWidth > 0 && sourceHeight > 0)
        require(compositionWidth > 0 && compositionHeight > 0)

        val widthScale = compositionWidth.toFloat() / sourceWidth
        val heightScale = compositionHeight.toFloat() / sourceHeight
        val scale = when (cropMode) {
            CropMode.COVER -> max(widthScale, heightScale)
            CropMode.CONTAIN -> min(widthScale, heightScale)
        }
        val drawWidth = sourceWidth * scale
        val drawHeight = sourceHeight * scale
        val clampedFocusX = focusX.coerceIn(0f, 1f)
        val clampedFocusY = focusY.coerceIn(0f, 1f)
        val left = (compositionWidth - drawWidth) * clampedFocusX
        val top = (compositionHeight - drawHeight) * clampedFocusY

        return FloatRect(left, top, left + drawWidth, top + drawHeight)
    }

    /** Projects the composition rectangle into one tile, including that tile's inner padding. */
    fun tileDrawRect(
        compositionRect: FloatRect,
        row: Int,
        column: Int,
        tileSize: Int,
        inset: Int,
    ): FloatRect {
        require(tileSize > 0)
        require(row >= 0 && column >= 0)
        require(inset in 0 until tileSize / 2)

        val innerSize = tileSize - inset * 2f
        val factor = innerSize / tileSize
        val tileLeft = column * tileSize
        val tileTop = row * tileSize
        return FloatRect(
            left = inset + (compositionRect.left - tileLeft) * factor,
            top = inset + (compositionRect.top - tileTop) * factor,
            right = inset + (compositionRect.right - tileLeft) * factor,
            bottom = inset + (compositionRect.bottom - tileTop) * factor,
        )
    }
}
