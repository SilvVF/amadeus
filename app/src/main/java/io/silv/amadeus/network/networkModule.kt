package io.silv.amadeus.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.silv.amadeus.network.mangadex.MangaDexApi
import io.silv.amadeus.network.mangadex.MangaDexTestApi
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {

    single {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    json = get<Json>(),
                    contentType = ContentType.Any
                )
            }
        }
    }

    single {
        MangaDexTestApi(get(), get())
    }

    single {
        MangaDexApi(get(), get())
    }
}