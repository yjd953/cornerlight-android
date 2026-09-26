package com.ninegrid.app.data.image

import android.content.Context
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.ninegrid.app.core.image.ImageSampling
import com.ninegrid.app.core.model.LocalizedMessage
import com.ninegrid.app.core.model.SourceImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.coroutineContext

/** Safely decodes one user-authorized content URI into a bounded, correctly oriented bitmap. */
class AndroidImageLoader(
    context: Context,
) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val cacheDir: File = File(context.cacheDir, "imports")

    suspend fun load(uri: Uri): SourceImage = withContext(Dispatchers.IO) {
        val metadata = queryMetadata(uri)
        if (metadata.size != null && metadata.size > MAX_FILE_SIZE) {
            throw ImageLoadException(
                "图片超过 50MB，请先压缩后再试",
                "The image is over 50 MB. Compress it and try again",
            )
        }

        val localCopy = copyToCache(uri)
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(localCopy.absolutePath, bounds)

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw ImageLoadException(
                    "当前设备无法解析这张图片，请尝试选择 JPG 或 PNG",
                    "This device cannot read the image. Try a JPG or PNG file",
                )
            }
            if (bounds.outWidth.toLong() * bounds.outHeight > MAX_IMAGE_PIXELS) {
                throw ImageLoadException(
                    "图片像素超过 8000 万，请先缩小后再试",
                    "The image exceeds 80 megapixels. Resize it and try again",
                )
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = ImageSampling.calculate(bounds.outWidth, bounds.outHeight, MAX_DECODE_EDGE)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = try {
                BitmapFactory.decodeFile(localCopy.absolutePath, options)
            } catch (_: OutOfMemoryError) {
                throw ImageLoadException(
                    "图片尺寸过大，设备内存不足",
                    "The image is too large for the available device memory",
                )
            } ?: throw ImageLoadException(
                "图片解码失败，请换一张 JPG、PNG、WebP 或 HEIC 图片",
                "Unable to decode the image. Try a JPG, PNG, WebP, or HEIC file",
            )

            val orientation = runCatching {
                ExifInterface(localCopy).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
            val oriented = applyOrientation(decoded, orientation)

            val sourceImage = SourceImage(
                bitmap = oriented,
                displayName = metadata.name ?: "Image",
                originalWidth = bounds.outWidth,
                originalHeight = bounds.outHeight,
                fileSize = metadata.size ?: localCopy.length(),
            )
            try {
                coroutineContext.ensureActive()
                sourceImage
            } catch (error: Throwable) {
                if (!oriented.isRecycled) oriented.recycle()
                throw error
            }
        } finally {
            localCopy.delete()
        }
    }

    private fun copyToCache(uri: Uri): File {
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw ImageLoadException(
                "无法创建图片缓存，请检查设备存储空间",
                "Unable to create image cache. Check the available storage",
            )
        }
        val target = File.createTempFile("selected-", ".image", cacheDir)
        try {
            openImageStream(uri).use { source ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_FILE_SIZE) {
                            throw ImageLoadException(
                                "图片超过 50MB，请先压缩后再试",
                                "The image is over 50 MB. Compress it and try again",
                            )
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (target.length() == 0L) {
                throw ImageLoadException(
                    "相册返回了空图片，请先将照片下载到本机后重试",
                    "The selected image is empty. Download it to this device and try again",
                )
            }
            return target
        } catch (error: CancellationException) {
            target.delete()
            throw error
        } catch (error: ImageLoadException) {
            target.delete()
            throw error
        } catch (_: FileNotFoundException) {
            target.delete()
            throw ImageLoadException(
                "找不到这张照片，云端照片请先下载到本机后重试",
                "The image was not found. Download cloud photos to this device first",
            )
        } catch (_: IOException) {
            target.delete()
            throw ImageLoadException(
                "读取或缓存照片失败，请检查存储空间后重试",
                "Unable to read or cache the image. Check the available storage",
            )
        } catch (_: RuntimeException) {
            target.delete()
            throw ImageLoadException(
                "系统相册返回了无法读取的照片，请换一张或使用其他相册入口",
                "The photo provider returned an unreadable image. Try another image or provider",
            )
        }
    }

    private fun openImageStream(uri: Uri): InputStream = try {
        contentResolver.openInputStream(uri)
            ?: throw ImageLoadException(
                "系统相册没有返回照片内容，请先下载原图后重试",
                "The photo provider returned no content. Download the original and try again",
            )
    } catch (_: SecurityException) {
        throw ImageLoadException(
            "照片读取授权已失效，请重新选择这张照片",
            "Photo access has expired. Select the image again",
        )
    } catch (_: FileNotFoundException) {
        throw ImageLoadException(
            "找不到这张照片，云端照片请先下载到本机后重试",
            "The image was not found. Download cloud photos to this device first",
        )
    } catch (_: IllegalArgumentException) {
        throw ImageLoadException(
            "系统相册返回了无效照片，请换一张或使用其他相册入口",
            "The photo provider returned an invalid image. Try another image or provider",
        )
    } catch (_: UnsupportedOperationException) {
        throw ImageLoadException(
            "当前相册不支持直接读取这张照片，请先保存到本机",
            "This provider cannot open the image directly. Save it to the device first",
        )
    }

    private fun queryMetadata(uri: Uri): ImageMetadata {
        var name: String? = null
        var size: Long? = null
        // Metadata is optional: clipboard providers and third-party document providers may reject
        // OpenableColumns even though opening the image stream itself is valid.
        try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: RuntimeException) {
            // Metadata is optional. The image stream can still be valid.
        }
        return ImageMetadata(name, size)
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (orientation == ExifInterface.ORIENTATION_NORMAL || matrix.isIdentity) return bitmap

        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { transformed -> if (transformed !== bitmap) bitmap.recycle() }
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    private data class ImageMetadata(val name: String?, val size: Long?)

    companion object {
        private const val MAX_FILE_SIZE = 50L * 1024 * 1024
        private const val MAX_IMAGE_PIXELS = 80_000_000L
        private const val MAX_DECODE_EDGE = 4096
    }
}

class ImageLoadException(
    override val chineseMessage: String,
    override val englishMessage: String,
) : Exception(chineseMessage), LocalizedMessage
