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
import io.silv.network.util.dohCloudflare
import io.silv.network.util.rateLimit
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
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
                    preconfigured = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .callTimeout(1, TimeUnit.MINUTES)
                    .rateLimit(permits = 40, period = 1, unit = TimeUnit.MINUTES)
                    .apply {
                        dohCloudflare()
                    }
                    .build()
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
                engine { preconfigured = get() }
                install(HttpCache) {
                    publicStorage(
                        DefaultHttpCache(
                            directory = File(get<Context>().cacheDir, "network_cache"),
                            // 5 MiB
                            maxSize = 5L * 1024 * 1024,
                        )
                    )
                }
                install(ContentNegotiation) {
                    json(
                        json = get(),
                        contentType = ContentType.Any,
                    )
                }
            }
        }

        single {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(1, TimeUnit.MINUTES)
                .rateLimit(permits = 300, period = 1, unit = TimeUnit.MINUTES)
                .apply {
                    dohCloudflare()
                }
                .build()
        }

        singleOf(::ImageSourceFactory)

        single {
            HttpSource(
                get(),
                HttpClient(OkHttp) {
                    engine {
                        preconfigured = OkHttpClient.Builder()
                            .callTimeout(1, TimeUnit.MINUTES)
                            .cache(
                                Cache(
                                    File(get<Context>().cacheDir, "chapter_url_cache"),
                                    2L * 1024 * 1024
                                )
                            )
                            .apply {
                                dohCloudflare()
                            }
                            .build()
                    }
                }
            )
        }

        single {
            MangaDexApi(get(), get(), get())
        }
    }
