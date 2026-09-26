package com.ninegrid.app.core.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class SourceImage(
    val bitmap: Bitmap,
    val displayName: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val fileSize: Long?,
)
