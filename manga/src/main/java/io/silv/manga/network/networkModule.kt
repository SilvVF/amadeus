package io.silv.manga.network

import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.silv.ktor_response_mapper.client.KSandwichClient
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.network.mangadex.MangaDexTestApi
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
        KSandwichClient.createClient(CIO) {
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