package com.ninegrid.app.core.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class TilePreview(
    val index: Int,
    val row: Int,
    val column: Int,
    val bitmap: Bitmap,
)
