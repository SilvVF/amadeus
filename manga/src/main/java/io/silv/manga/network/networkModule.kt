package io.silv.manga.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.silv.ktor_response_mapper.client.KSandwichClient
import io.silv.manga.local.workers.handlers.AzukiHandler
import io.silv.manga.local.workers.handlers.BiliHandler
import io.silv.manga.local.workers.handlers.ComikeyHandler
import io.silv.manga.local.workers.handlers.MangaHotHandler
import io.silv.manga.local.workers.handlers.MangaPlusHandler
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.MangaDexTestApi
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

    factoryOf(::AzukiHandler)
    factoryOf(::MangaHotHandler)
    factoryOf(::MangaPlusHandler)
    factoryOf(::ComikeyHandler)
    factoryOf(::BiliHandler)

    single {
        KSandwichClient.create(get())
    }

    single {
        MangaDexTestApi(get(), get())
    }

    single {
        MangaDexApi(get(), get())
    }
}