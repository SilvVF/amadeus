package io.silv.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import okio.Buffer
import okio.BufferedSource
import java.io.InputStream
import java.net.URLConnection

object ImageUtil {


    fun isWideImage(imageSource: BufferedSource): Boolean {
        val options = extractImageOptions(imageSource)
        return options.outWidth > options.outHeight
    }

    private fun extractImageOptions(imageSource: BufferedSource): BitmapFactory.Options {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(imageSource.peek().inputStream(), null, options)
        return options
    }


    fun findImageType(openStream: () -> InputStream): ImageType? {
        return openStream().use { findImageType(it) }
    }

    fun getMimeType(inputStream: InputStream): String? {
        inputStream.mark(1024)
        val mimeType = URLConnection.guessContentTypeFromStream(inputStream)
        inputStream.reset()
        return mimeType
    }

    fun findImageType(stream: InputStream): ImageType? {
        return try {
            when (getMimeType(stream)) {
                "image/avif" -> ImageType.AVIF
                "image/gif" -> ImageType.GIF
                "image/heif", "image/heic" -> ImageType.HEIF
                "image/jpeg" -> ImageType.JPEG
                "image/jxl" -> ImageType.JXL
                "image/png" -> ImageType.PNG
                "image/webp" -> ImageType.WEBP
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    fun getExtensionFromMimeType(mime: String?, openStream: () -> InputStream): String {
        val type = mime?.let { ImageType.entries.find { it.mime == mime } } ?: findImageType(openStream)
        return type?.extension ?: "jpg"
    }

    enum class ImageType(val mime: String, val extension: String) {
        AVIF("image/avif", "avif"),
        GIF("image/gif", "gif"),
        HEIF("image/heif", "heif"),
        JPEG("image/jpeg", "jpg"),
        JXL("image/jxl", "jxl"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
    }


    /**
     * Extract the 'side' part from [BufferedSource] and return it as [BufferedSource].
     */
    fun splitInHalf(imageSource: BufferedSource, side: Side): BufferedSource {
        val imageBitmap = BitmapFactory.decodeStream(imageSource.inputStream())
        val height = imageBitmap.height
        val width = imageBitmap.width

        val singlePage = Rect(0, 0, width / 2, height)

        val half = createBitmap(width / 2, height)
        val part = when (side) {
            Side.RIGHT -> Rect(width - width / 2, 0, width, height)
            Side.LEFT -> Rect(0, 0, width / 2, height)
        }
        half.applyCanvas {
            drawBitmap(imageBitmap, part, singlePage, null)
        }
        val output = Buffer()
        half.compress(Bitmap.CompressFormat.JPEG, 100, output.outputStream())

        return output
    }

    enum class Side {
        RIGHT,
        LEFT,
    }
}
