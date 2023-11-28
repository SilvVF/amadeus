package io.silv.data.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import io.silv.common.AmadeusDispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@Suppress("blocking call")
internal class ImageDownloader(
    private val context: Context,
    private val dispatchers: AmadeusDispatchers
) {
    suspend fun writeMangaCoverArt(
        mangaId: String,
        url: String,
    ) = withContext(dispatchers.io) {
        val image = URL(url)
        val inputStream = image.openStream().buffered()
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val ext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "webp" else "png"

        val fileName = "cover_art-$mangaId.$ext"

        val file = File(context.filesDir, fileName)

        file.outputStream().buffered().use {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it)
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

        file.toUri()
    }

    suspend fun write(
        mangaId: String,
        chapterId: String,
        pageNumber: Int,
        url: String
    ): Uri = withContext(dispatchers.io) {

        val image = URL(url)
        val inputStream = image.openStream().buffered()
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val ext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "webp" else "png"

        val fileName = "chapter_images-$mangaId-$chapterId-$pageNumber.$ext"

        val file = File(context.filesDir, fileName)

        file.outputStream().buffered().use {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it)
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

        file.toUri()
    }
}