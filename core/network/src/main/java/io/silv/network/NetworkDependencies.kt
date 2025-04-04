package io.silv.network

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.silv.common.DependencyAccessor
import io.silv.common.commonDeps
import io.silv.network.util.bucket.TokenBucketPlugin
import io.silv.network.util.dohCloudflare
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit

@DependencyAccessor
public lateinit var networkDeps: NetworkDependencies

@OptIn(DependencyAccessor::class)
abstract class NetworkDependencies {

    abstract val context: Context

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    val atHomeClient: AtHomeClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(15, TimeUnit.SECONDS)
                    dohCloudflare()
                }
            }
            install(TokenBucketPlugin) {
                bucket = {
                    withCapacity(40L)
                    withInitialTokens(40L)
                    withFixedIntervalRefillStrategy(
                        refillTokens = 40L,
                        periodNanos = 60 * 1_000_000_000L
                    )
                }
            }
            install(ContentNegotiation) {
                json(
                    json = json,
                    contentType = ContentType.Any,
                )
            }
        }
    }

    val mangaDexClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(15, TimeUnit.SECONDS)
                    callTimeout(1, TimeUnit.MINUTES)
                    dohCloudflare()
                }
            }
            install(HttpCache) {
                publicStorage(
                    DefaultHttpCache(
                        directory = File(context.cacheDir, "network_cache"),
                        // 5 MiB
                        maxSize = 5L * 1024 * 1024,
                    )
                )
            }
            install(TokenBucketPlugin) {
                bucket = {
                    withCapacity(300L)
                    withInitialTokens(300L)
                    withFixedIntervalRefillStrategy(
                        refillTokens = 300L,
                        periodNanos = 60 * 1_000_000_000L
                    )
                }
            }
            install(ContentNegotiation) {
                json(
                    json = json,
                    contentType = ContentType.Any,
                )
            }
        }
    }

    val mangaDexApi by lazy {
        MangaDexApi(mangaDexClient, atHomeClient, commonDeps.dispatchers)
    }
}