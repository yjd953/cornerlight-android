package com.ninegrid.app.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ninegrid.app.core.model.AppLanguage

class ShareLauncher(private val context: Context) {
    fun shareImages(uris: List<Uri>, language: AppLanguage) {
        require(uris.isNotEmpty())
        val action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
        val intent = Intent(action).apply {
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, language.text("隅光", "Cornerlight"))
            putExtra(
                Intent.EXTRA_TEXT,
                language.text(
                    "图片已按发布顺序编号",
                    "Images are numbered in publishing order",
                ),
            )
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            clipData = ClipData.newRawUri("cornerlight", uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                language.text("分享切图", "Share images"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
