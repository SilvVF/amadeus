package io.silv.data.download

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.FileUtils
import com.hippo.unifile.UniFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.use
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resumeWithException

val getDisplayMaxHeightInPx: Int
    get() = Resources.getSystem().displayMetrics.let { kotlin.math.max(it.heightPixels, it.widthPixels) }

val UniFile.extension: String?
    get() = name?.substringAfterLast('.')

val UniFile.nameWithoutExtension: String?
    get() = name?.substringBeforeLast('.')

fun UniFile.toTempFile(context: Context): File {
    val inputStream = context.contentResolver.openInputStream(uri)!!
    val tempFile = File.createTempFile(
        nameWithoutExtension.orEmpty(),
        null,
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        FileUtils.copy(inputStream, tempFile.outputStream())
    } else {
        BufferedOutputStream(tempFile.outputStream()).use { tmpOut ->
            inputStream.use { input ->
                val buffer = ByteArray(8192)
                var count: Int
                while (input.read(buffer).also { count = it } > 0) {
                    tmpOut.write(buffer, 0, count)
                }
            }
        }
    }

    return tempFile
}
// Based on https://github.com/gildor/kotlin-coroutines-okhttp
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Call.await(callStack: Array<StackTraceElement>): Response {
    return suspendCancellableCoroutine { continuation ->
        val callback =
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) { _, _, _->
                        response.body?.close()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    val exception = IOException(e.message, e).apply { stackTrace = callStack }
                    continuation.resumeWithException(exception)
                }
            }

        enqueue(callback)

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Ignore cancel exception
            }
        }
    }
}

suspend fun Call.await(): Response {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    return await(callStack)
}

/**
 * Saves the given source to a file and closes it. Directories will be created if needed.
 *
 * @param file the file where the source is copied.
 */
fun BufferedSource.saveTo(file: File) {
    try {
        // Create parent dirs if needed
        file.parentFile?.mkdirs()

        // Copy to destination
        saveTo(file.outputStream())
    } catch (e: Exception) {
        close()
        file.delete()
        throw e
    }
}

/**
 * Saves the given source to an output stream and closes both resources.
 *
 * @param stream the stream where the source is copied.
 */
fun BufferedSource.saveTo(stream: OutputStream) {
    use { input ->
        stream.sink().buffer().use {
            it.writeAll(input)
            it.flush()
        }
    }
}