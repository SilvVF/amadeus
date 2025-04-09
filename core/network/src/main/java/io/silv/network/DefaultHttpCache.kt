package io.silv.network

import android.util.Log
import com.mayakapps.kache.FileKache
import com.mayakapps.kache.KacheStrategy
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import io.ktor.util.flattenEntries
import io.ktor.util.hex
import io.silv.common.log.logcat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.internal.discard
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

internal class DefaultHttpCache internal constructor(
    directory: File,
    // 5 MiB
    maxSize: Long = 5L * 1024 * 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CacheStorage by LruCacheStorage(
    {
        FileKache(directory = directory.path, maxSize = maxSize) {
            strategy = KacheStrategy.LRU
        }
    },
    dispatcher = dispatcher
)

private class LruCacheStorage(
    private val diskCacheInit: suspend () -> FileKache,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CacheStorage {

    fun canCache(url: Url): Boolean {
        val urlString = url.toString()
        return !(urlString.startsWith("https://uploads.mangadex.org/covers") ||
                urlString.startsWith("https://api.mangadex.org/at-home/server/") ||
                urlString.contains(".png") || urlString.contains(".jpg")) || urlString.contains(".webp")
    }

    /** Cache class used for cache management. */
    private val diskCache by lazy {
        runBlocking {
            diskCacheInit()
        }
    }

    override suspend fun store(url: Url, data: CachedResponseData): Unit = withContext(dispatcher) {

        val urlHex = key(url)
        val caches = readCache(urlHex)
            .filterNot { it.varyKeys == data.varyKeys } + data
        Log.d(
            "HttpCacheImpl",
            "writing to cache caches $url"
        )
        writeCache(urlHex, caches)
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> {
        // even with no-cache headers still trying to read from cache
        // doesn't try to write to cache though
        if (!canCache(url)) {
            return emptySet()
        }

        Log.d(
            "HttpCacheImpl",
            "findAll from cache $url"
        )
        return readCache(key(url)).toSet().also {
            Log.d(
                "HttpCacheImpl",
                "found from findAll $it"
            )
        }
    }

    override suspend fun find(url: Url, varyKeys: Map<String, String>): CachedResponseData? {
        if (!canCache(url)) {
            return null
        }
        Log.d(
            "HttpCacheImpl",
            "find from cache $url"
        )
        val data = readCache(key(url))
        return data.find {
            varyKeys.all { (key, value) -> it.varyKeys[key] == value }
        }.also {
            Log.d(
                "HttpCacheImpl",
                "found ${it?.url.toString()}"
            )
        }
    }

    private fun key(url: Url) =
        hex(MessageDigest.getInstance("MD5").digest(url.toString().encodeToByteArray()))

    private suspend fun writeCache(
        urlHex: String,
        caches: List<CachedResponseData>
    ) = coroutineScope {
        logcat { "writing to cache $urlHex" }
        // Initialize the editor (edits the values for an entry).
        try {
            diskCache.put(urlHex) { fileName ->
                runCatching {

                    fileSystem.sink(fileName.toPath()).buffer().use { sink ->

                        sink.writeInt(caches.size)

                        for (cache in caches) {
                            writeCache(sink, cache)
                        }
                    }
                }
                    .isSuccess
            }
        } catch (cause: Exception) {
            Log.e(
                "HttpCacheImpl",
                "Exception during saving a cache to a file: ${cause.message}"
            )
        }
    }

    private suspend fun readCache(urlHex: String): Set<CachedResponseData> {
        return try {
            val container = diskCache.get(urlHex)!!
            val source = fileSystem.source(container.toPath()).buffer()
            val requestsCount = source.readInt()
            val caches = mutableSetOf<CachedResponseData>()
            repeat(requestsCount) {
                caches.add(readCache(source))
            }
            source.discard(10, TimeUnit.SECONDS)
            caches
        } catch (cause: Exception) {
            logcat { "Exception during reading a file: ${cause.message}" }
            emptySet()
        }
    }
}

private fun writeCache(sink: BufferedSink, cache: CachedResponseData) {
    sink.writeUtf8(cache.url.toString() + "\n")
    sink.writeInt(cache.statusCode.value)
    sink.writeUtf8(cache.statusCode.description + "\n")
    sink.writeUtf8(cache.version.toString() + "\n")
    val headers = cache.headers.flattenEntries()
    sink.writeInt(headers.size)
    for ((key, value) in headers) {
        sink.writeUtf8(key + "\n")
        sink.writeUtf8(value + "\n")
    }
    sink.writeLong(cache.requestTime.timestamp)
    sink.writeLong(cache.responseTime.timestamp)
    sink.writeLong(cache.expires.timestamp)
    sink.writeInt(cache.varyKeys.size)
    for ((key, value) in cache.varyKeys) {
        sink.writeUtf8(key + "\n")
        sink.writeUtf8(value + "\n")
    }
    sink.writeInt(cache.body.size)
    sink.write(cache.body)
}

private fun readCache(source: BufferedSource): CachedResponseData {
    val url = source.readUtf8Line()!!
    val status = HttpStatusCode(source.readInt(), source.readUtf8Line()!!)
    val version = HttpProtocolVersion.parse(source.readUtf8Line()!!)
    val headersCount = source.readInt()
    val headers = HeadersBuilder()
    for (j in 0 until headersCount) {
        val key = source.readUtf8Line()!!
        val value = source.readUtf8Line()!!
        headers.append(key, value)
    }
    val requestTime = GMTDate(source.readLong())
    val responseTime = GMTDate(source.readLong())
    val expirationTime = GMTDate(source.readLong())
    val varyKeysCount = source.readInt()
    val varyKeys = buildMap {
        for (j in 0 until varyKeysCount) {
            val key = source.readUtf8Line()!!
            val value = source.readUtf8Line()!!
            put(key, value)
        }
    }
    val bodyCount = source.readInt()
    val body = ByteArray(bodyCount)
    source.readFully(body)
    return CachedResponseData(
        url = Url(url),
        statusCode = status,
        requestTime = requestTime,
        responseTime = responseTime,
        version = version,
        expires = expirationTime,
        headers = headers.build(),
        varyKeys = varyKeys,
        body = body
    )
}

