package io.silv.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.silv.ktor_response_mapper.client.KSandwichClient
import io.silv.network.image_sources.ImageSourceFactory
import io.silv.network.util.dohCloudflare
import io.silv.network.util.rateLimit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import java.util.concurrent.TimeUnit


val networkModule = module {

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
                preconfigured = get<OkHttpClient>()
            }
            install(ContentNegotiation) {
                json(
                    json = get<Json>(),
                    contentType = ContentType.Any
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
        KSandwichClient.create(get())
    }

    single {
        MangaDexApi(get(), get())
    }
}