package io.silv.amadeus

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.StatFs
import com.hippo.unifile.UniFile
import java.io.File
import java.security.MessageDigest

object Hash {

    private val chars = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f',
    )

    private val MD5 get() = MessageDigest.getInstance("MD5")

    private val SHA256 get() = MessageDigest.getInstance("SHA-256")

    fun sha256(bytes: ByteArray): String {
        return encodeHex(SHA256.digest(bytes))
    }

    fun sha256(string: String): String {
        return sha256(string.toByteArray())
    }

    fun md5(bytes: ByteArray): String {
        return encodeHex(MD5.digest(bytes))
    }

    fun md5(string: String): String {
        return md5(string.toByteArray())
    }

    private fun encodeHex(data: ByteArray): String {
        val l = data.size
        val out = CharArray(l shl 1)
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = chars[(240 and data[i].toInt()).ushr(4)]
            out[j++] = chars[15 and data[i].toInt()]
            i++
        }
        return String(out)
    }
}

object DiskUtil {

    fun hashKeyForDisk(key: String): String {
        return Hash.md5(key)
    }

    fun getDirectorySize(f: File): Long {
        var size: Long = 0
        if (f.isDirectory) {
            for (file in f.listFiles().orEmpty()) {
                size += getDirectorySize(file)
            }
        } else {
            size = f.length()
        }
        return size
    }

    /**
     * Gets the total space for the disk that a file path points to, in bytes.
     */
    fun getTotalStorageSpace(file: File): Long {
        return try {
            val stat = StatFs(file.absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Gets the available space for the disk that a file path points to, in bytes.
     */
    fun getAvailableStorageSpace(file: File): Long {
        return try {
            val stat = StatFs(file.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Gets the available space for the disk that a file path points to, in bytes.
     */
    fun getAvailableStorageSpace(f: UniFile): Long {
        return try {
            val stat = StatFs(f.uri.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Don't display downloaded chapters in gallery apps creating `.nomedia`.
     */
    fun createNoMediaFile(dir: UniFile?, context: Context?) {
        if (dir != null && dir.exists()) {
            val nomedia = dir.findFile(NOMEDIA_FILE)
            if (nomedia == null) {
                dir.createFile(NOMEDIA_FILE)
                context?.let { scanMedia(it, dir.uri) }
            }
        }
    }

    /**
     * Scans the given file so that it can be shown in gallery apps, for example.
     */
    fun scanMedia(context: Context, uri: Uri) {
        MediaScannerConnection.scanFile(context, arrayOf(uri.path), null, null)
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_". This method doesn't allow hidden files (starting
     * with a dot), but you can manually add it later.
     */
    fun buildValidFilename(origName: String): String {
        val name = origName.trim('.', ' ')
        if (name.isEmpty()) {
            return "(invalid)"
        }
        val sb = StringBuilder(name.length)
        name.forEach { c ->
            if (isValidFatFilenameChar(c)) {
                sb.append(c)
            } else {
                sb.append('_')
            }
        }
        // Even though vfat allows 255 UCS-2 chars, we might eventually write to
        // ext4 through a FUSE layer, so use that limit minus 15 reserved characters.
        return sb.toString().take(240)
    }

    /**
     * Returns true if the given character is a valid filename character, false otherwise.
     */
    private fun isValidFatFilenameChar(c: Char): Boolean {
        if (0x00.toChar() <= c && c <= 0x1f.toChar()) {
            return false
        }
        return when (c) {
            '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7f.toChar() -> false
            else -> true
        }
    }

    const val NOMEDIA_FILE = ".nomedia"

    // Safe theoretical max filename size is 255 bytes and 1 char = 2-4 bytes (UTF-8)
    const val MAX_FILE_NAME_BYTES = 250
}