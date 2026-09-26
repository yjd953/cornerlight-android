package com.ninegrid.app.core.image

import kotlin.math.max

/** Pure sampling policy used before decoding large source images. */
object ImageSampling {
    /**
     * Returns the smallest power-of-two `inSampleSize` whose longest decoded edge does not exceed
     * [maxEdge]. Powers of two retain predictable behaviour across older Android decoders.
     */
    fun calculate(width: Int, height: Int, maxEdge: Int): Int {
        require(width > 0 && height > 0) { "Image dimensions must be positive" }
        require(maxEdge > 0) { "Maximum edge must be positive" }

        var sampleSize = 1
        while (max(width / sampleSize, height / sampleSize) > maxEdge) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
