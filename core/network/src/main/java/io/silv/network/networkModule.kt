package io.silv.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.silv.network.sources.ImageSourceFactory
import io.silv.network.util.dohCloudflare
import io.silv.network.util.rateLimit
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val networkModule =
    module {

        single {
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
            }
        }

        single {
            HttpClient(OkHttp) {
                engine {
                    preconfigured = get()
                }
                install(HttpCache)
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

        factoryOf(::ImageSourceFactory)

        single {
            MangaDexApi(get(), get())
        }
    }
