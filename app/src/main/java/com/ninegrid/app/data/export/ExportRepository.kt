package com.ninegrid.app.data.export

import android.content.ContentValues
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.ninegrid.app.core.image.GridRenderer
import com.ninegrid.app.core.model.EditorSettings
import com.ninegrid.app.core.model.LocalizedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

/**
 * Writes rendered tiles to platform-owned destinations.
 *
 * Tiles are rendered and encoded one at a time. This deliberately trades a little CPU time for a
 * low, stable memory peak on entry-level devices.
 */
class ExportRepository(
    private val context: Context,
    private val renderer: GridRenderer,
) {
    val requiresLegacyWritePermission: Boolean
        get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED

    suspend fun saveAllToGallery(
        source: android.graphics.Bitmap,
        settings: EditorSettings,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): List<Uri> = withContext(Dispatchers.IO) {
        val session = ExportFileNames.session()
        val saved = mutableListOf<Uri>()
        try {
            forEachTile(settings) { index, row, column ->
                coroutineContext.ensureActive()
                val fileName = ExportFileNames.tile(session, index, row, column)
                saved += saveRenderedTile(source, settings, row, column, fileName)
                onProgress(index + 1, settings.gridSpec.tileCount)
            }
            saved
        } catch (error: Throwable) {
            saved.asReversed().forEach(::deleteSavedTile)
            throw error
        }
    }

    suspend fun saveOneToGallery(
        source: android.graphics.Bitmap,
        settings: EditorSettings,
        index: Int,
    ): Uri = withContext(Dispatchers.IO) {
        require(index in 0 until settings.gridSpec.tileCount)
        val row = index / settings.gridSpec.columns
        val column = index % settings.gridSpec.columns
        saveRenderedTile(
            source = source,
            settings = settings,
            row = row,
            column = column,
            fileName = ExportFileNames.tile(ExportFileNames.session(), index, row, column),
        )
    }

    suspend fun prepareShareImages(
        source: android.graphics.Bitmap,
        settings: EditorSettings,
        onlyIndex: Int? = null,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): List<Uri> = withContext(Dispatchers.IO) {
        val shareRoot = File(context.cacheDir, "share").apply {
            if (!exists() && !mkdirs()) {
                throw ExportException("无法创建分享缓存", "Unable to create the sharing cache")
            }
        }
        removeExpiredShareDirectories(shareRoot)
        val session = ExportFileNames.session()
        val shareDir = File(shareRoot, "$session-${System.nanoTime()}").apply {
            if (!mkdirs()) {
                throw ExportException("无法创建分享缓存", "Unable to create the sharing cache")
            }
        }
        val indices = onlyIndex?.let(::listOf) ?: (0 until settings.gridSpec.tileCount).toList()
        try {
            indices.mapIndexed { progressIndex, index ->
                coroutineContext.ensureActive()
                require(index in 0 until settings.gridSpec.tileCount)
                val row = index / settings.gridSpec.columns
                val column = index % settings.gridSpec.columns
                val file = File(shareDir, ExportFileNames.tile(session, index, row, column))
                FileOutputStream(file).use { output ->
                    renderToStream(source, settings, row, column, output)
                }
                onProgress(progressIndex + 1, indices.size)
                FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            }
        } catch (error: Throwable) {
            shareDir.deleteRecursively()
            throw error
        }
    }

    suspend fun writeZip(
        destination: Uri,
        source: android.graphics.Bitmap,
        settings: EditorSettings,
        onProgress: (completed: Int, total: Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(destination, "w")
            ?: throw ExportException("无法创建 ZIP 文件", "Unable to create the ZIP file")
        output.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                val session = ExportFileNames.session()
                forEachTile(settings) { index, row, column ->
                    coroutineContext.ensureActive()
                    zip.putNextEntry(ZipEntry(ExportFileNames.tile(session, index, row, column)))
                    renderToStream(source, settings, row, column, zip)
                    zip.closeEntry()
                    onProgress(index + 1, settings.gridSpec.tileCount)
                }
            }
        }
    }

    fun zipFileName(): String = ExportFileNames.zip(ExportFileNames.session())

    private fun saveRenderedTile(
        source: android.graphics.Bitmap,
        settings: EditorSettings,
        row: Int,
        column: Int,
        fileName: String,
    ): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveModern(source, settings, row, column, fileName)
    } else {
        saveLegacy(source, settings, row, column, fileName)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun saveModern(
        source: android.graphics.Bitmap,
        settings: EditorSettings,
        row: Int,
        column: Int,
        fileName: String,
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, JPEG_MIME)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values)
            ?: throw ExportException(
                "无法在相册中创建图片",
                "Unable to create an image in the gallery",
            )
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                renderToStream(source, settings, row, column, output)
            } ?: throw ExportException("无法写入相册", "Unable to write to the gallery")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            return uri
        } catch (error: Throwable) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(
        source: android.graphics.Bitmap,
        settings: EditorSettings,
        row: Int,
        column: Int,
        fileName: String,
    ): Uri {
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val album = File(pictures, ALBUM_NAME).apply {
            if (!exists() && !mkdirs()) {
                throw ExportException("无法创建相册目录", "Unable to create the gallery folder")
            }
        }
        val file = File(album, fileName)
        FileOutputStream(file).use { output -> renderToStream(source, settings, row, column, output) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(JPEG_MIME), null)
        return Uri.fromFile(file)
    }

    private fun renderToStream(
        source: android.graphics.Bitmap,
        settings: EditorSettings,
        row: Int,
        column: Int,
        output: OutputStream,
    ) {
        val bitmap = renderer.renderTile(source, settings, row, column)
        try {
            if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, settings.quality, output)) {
                throw ExportException("图片编码失败", "Unable to encode the image")
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun deleteSavedTile(uri: Uri) {
        runCatching {
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> context.contentResolver.delete(uri, null, null)
                ContentResolver.SCHEME_FILE -> {
                    val path = uri.path ?: return@runCatching
                    File(path).delete()
                    MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(JPEG_MIME), null)
                }
            }
        }
    }

    private fun removeExpiredShareDirectories(root: File) {
        val cutoff = System.currentTimeMillis() - SHARE_CACHE_MAX_AGE_MS
        root.listFiles()
            ?.filter { it.isDirectory && it.lastModified() < cutoff }
            ?.forEach { it.deleteRecursively() }
    }

    private inline fun forEachTile(settings: EditorSettings, block: (Int, Int, Int) -> Unit) {
        repeat(settings.gridSpec.rows) { row ->
            repeat(settings.gridSpec.columns) { column ->
                block(row * settings.gridSpec.columns + column, row, column)
            }
        }
    }

    companion object {
        const val ALBUM_NAME = "隅光"
        private const val JPEG_MIME = "image/jpeg"
        private const val SHARE_CACHE_MAX_AGE_MS = 24L * 60 * 60 * 1000
    }
}

class ExportException(
    override val chineseMessage: String,
    override val englishMessage: String,
) : Exception(chineseMessage), LocalizedMessage
