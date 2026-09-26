package com.ninegrid.app.data.export

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ninegrid.app.core.image.GridRenderer
import com.ninegrid.app.core.model.EditorSettings
import com.ninegrid.app.core.model.GridSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class ExportRepositoryInstrumentedTest {
    @Test
    fun shareAndZipExportsContainEveryTileInPublicationOrder() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = ExportRepository(context, GridRenderer())
        val source = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.CYAN)
        }
        val settings = EditorSettings(gridSpec = GridSpec.FOUR, quality = 80)

        try {
            val shareUris = repository.prepareShareImages(source, settings)
            assertEquals(4, shareUris.size)
            shareUris.forEach { uri ->
                context.contentResolver.openInputStream(uri).use { input ->
                    assertTrue(input != null && input.read() >= 0)
                }
            }
            repository.prepareShareImages(source, settings, onlyIndex = 0)
            shareUris.forEach { uri ->
                context.contentResolver.openInputStream(uri).use { input ->
                    assertTrue(input != null && input.read() >= 0)
                }
            }

            val archiveDir = File(context.cacheDir, "archives").apply { mkdirs() }
            val archive = File(archiveDir, "export-test.zip")
            val archiveUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                archive,
            )
            repository.writeZip(archiveUri, source, settings) { _, _ -> }

            val entries = buildList {
                context.contentResolver.openInputStream(archiveUri)?.use { raw ->
                    ZipInputStream(raw).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            add(entry.name)
                            entry = zip.nextEntry
                        }
                    }
                }
            }
            assertEquals(4, entries.size)
            assertTrue(entries[0].startsWith("01_"))
            assertTrue(entries[3].startsWith("04_"))
            archive.delete()
        } finally {
            source.recycle()
        }
    }
}
