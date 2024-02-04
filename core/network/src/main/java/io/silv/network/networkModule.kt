package io.silv.network

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.silv.network.sources.HttpSource
import io.silv.network.sources.ImageSourceFactory
import io.silv.network.util.bucket.TokenBucketPlugin
import io.silv.network.util.dohCloudflare
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File
import java.util.concurrent.TimeUnit

val networkModule =
    module {
        single {
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
            }
        }

        single<AtHomeClient> {
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
                        withFixedIntervalRefillStrategy(
                            refillTokens = 40L,
                            periodNanos = 60 * 1_000_000_000L
                        )
                    }
                }
                install(ContentNegotiation) {
                    json(
                        json = get(),
                        contentType = ContentType.Any,
                    )
                }
            }
        }

        single<MangaDexClient> {
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
                            directory = File(get<Context>().cacheDir, "network_cache"),
                            // 5 MiB
                            maxSize = 5L * 1024 * 1024,
                        )
                    )
                }
                install(TokenBucketPlugin) {
                    bucket = {
                        withCapacity(300L)
                        withFixedIntervalRefillStrategy(
                            refillTokens = 300L,
                            periodNanos = 60 * 1_000_000_000L
                        )
                    }
                }
                install(ContentNegotiation) {
                    json(
                        json = get(),
                        contentType = ContentType.Any,
                    )
                }
            }
        }

        singleOf(::ImageSourceFactory)

        single {
            HttpSource(
                mangaDexApi = get(),
                client = HttpClient(OkHttp) {
                    install(HttpCache) {
                        publicStorage(
                            DefaultHttpCache(
                                File(get<Context>().cacheDir, "chapter_url_cache"),
                                2L * 1024 * 1024
                            )
                        )
                    }
                    engine {
                        config {
                            callTimeout(1, TimeUnit.MINUTES)
                            dohCloudflare()
                        }
                    }
                }
            )
        }

        single {
            MangaDexApi(get(), get(), get())
        }
    }
