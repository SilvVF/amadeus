

package io.silv.amadeus.local.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import io.silv.amadeus.AmadeusDispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL


class ChapterImageCache(
    private val context: Context,
    private val dispatchers: AmadeusDispatchers
) {


    suspend fun write(
        mangaId: String,
        volumeId: String,
        chapterId: String,
        pageNumber: Int,
        url: String
    ): Uri = withContext(dispatchers.io) {

        val image = URL(url)
        val inputStream = image.openStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val fileName = "$mangaId-$volumeId-$chapterId-$pageNumber.png"

        val file = File(context.filesDir, fileName)

        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        file.toUri().also { println("ttt" + it) }
    }
}