package com.ninegrid.app.core.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Immutable, validated parameters consumed identically by preview and final export. */
@Immutable
data class EditorSettings(
    val gridSpec: GridSpec = GridSpec.NINE,
    val cropMode: CropMode = CropMode.COVER,
    val paddingPercent: Float = 0f,
    val backgroundColor: Color = Color.White,
    val quality: Int = 90,
    val focusX: Float = 0.5f,
    val focusY: Float = 0.5f,
) {
    init {
        require(paddingPercent in 0f..12f)
        require(quality in 60..100)
        require(focusX in 0f..1f)
        require(focusY in 0f..1f)
    }

    companion object {
        val Default = EditorSettings()
    }
}
