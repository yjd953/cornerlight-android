package com.ninegrid.app.data.export

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Central naming policy shared by gallery, share and ZIP exports. */
object ExportFileNames {
    fun session(timestampMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestampMillis))

    fun tile(session: String, index: Int, row: Int, column: Int): String {
        require(session.isNotBlank()) { "Session name must not be blank" }
        require(index >= 0 && row >= 0 && column >= 0) { "Tile coordinates must not be negative" }
        return "%02d_%s_%d_%d.jpg".format(index + 1, session, row + 1, column + 1)
    }

    fun zip(session: String): String = "cornerlight-$session.zip"
}
