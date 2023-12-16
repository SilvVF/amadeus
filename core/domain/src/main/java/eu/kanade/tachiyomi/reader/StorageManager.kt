package eu.kanade.tachiyomi.reader

import android.content.Context
import android.util.Log
import com.hippo.unifile.UniFile
import java.io.File

class StorageManager(
    context: Context,
) {

    private var baseDir: UniFile? = UniFile.fromFile(
        File(context.getExternalFilesDir(null)!!.absolutePath + File.separator + "Amadeus").also { it.mkdirs() }
            .also { Log.d("BASEDIR", it.absolutePath) }
    )


    fun getDownloadsDirectory(): UniFile? {
        return baseDir?.createDirectory(DOWNLOADS_PATH)
    }

}

private const val DOWNLOADS_PATH = "downloads"