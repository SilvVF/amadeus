package io.silv.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import java.io.File

internal class StorageManager(
    context: Context,
) {

    private var baseDir: UniFile? = UniFile.fromFile(
        File(context.getExternalFilesDir(null)?.absolutePath + File.separator + "Amadeus")
    )


    fun getDownloadsDirectory(): UniFile? {
        return baseDir?.createDirectory(DOWNLOADS_PATH)
    }
}

private const val DOWNLOADS_PATH = "downloads"