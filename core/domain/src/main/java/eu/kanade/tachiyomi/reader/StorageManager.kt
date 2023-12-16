package eu.kanade.tachiyomi.reader

import android.content.Context
import com.hippo.unifile.UniFile

class StorageManager(
    context: Context,
) {

    private var baseDir: UniFile? = UniFile.fromFile(context.filesDir.absoluteFile)


    fun getDownloadsDirectory(): UniFile? {
        return baseDir?.createDirectory(DOWNLOADS_PATH)
    }

}

private const val DOWNLOADS_PATH = "downloads"