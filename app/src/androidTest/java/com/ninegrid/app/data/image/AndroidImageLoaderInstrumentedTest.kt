package com.ninegrid.app.data.image

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class AndroidImageLoaderInstrumentedTest {
    @Test
    fun decodesAFileProviderImageAndPreservesMetadata() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(directory, "loader-test.jpg")
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val source = AndroidImageLoader(context).load(uri)

        try {
            assertEquals(80, source.originalWidth)
            assertEquals(40, source.originalHeight)
            assertEquals("loader-test.jpg", source.displayName)
            assertFalse(source.bitmap.isRecycled)
        } finally {
            source.bitmap.recycle()
            file.delete()
        }
    }

    @Test
    fun rejectsAnEmptyProviderFileWithAReadableError() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(directory, "empty-image.jpg").apply { writeBytes(byteArrayOf()) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

        try {
            val error = runCatching { AndroidImageLoader(context).load(uri) }.exceptionOrNull()
            assertTrue(error is ImageLoadException)
            assertTrue(error?.message?.contains("空图片") == true)
        } finally {
            file.delete()
        }
    }
}
