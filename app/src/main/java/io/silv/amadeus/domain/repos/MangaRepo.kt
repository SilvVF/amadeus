package io.silv.amadeus.domain.repos

import io.silv.amadeus.domain.models.Manga
import io.silv.amadeus.network.mangadex.MangaDexApi
import io.silv.amadeus.network.mangadex.models.Group
import io.silv.amadeus.network.mangadex.models.manga.MangaListResponse
import io.silv.amadeus.network.mangadex.requests.MangaRequest
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.ApiSuccessModelMapper
import io.silv.ktor_response_mapper.mapSuccess

class MangaRepo(
    private val mangaDexApi: MangaDexApi
) {

    private var offset: Int = 0

    suspend fun getMangaWithArt(): ApiResponse<List<Manga>> {
        return mangaDexApi.getMangaList(
                MangaRequest(
                    limit = 30,
                    offset = offset,
                    includes = listOf("cover_art")
                )
            )
            .mapSuccess { this.data.map(::toDomain) }
    }

    private fun toDomain(networkManga: io.silv.amadeus.network.mangadex.models.manga.Manga): Manga {

        val fileName = networkManga.relationships.find {
            it.type == "cover_art"
        }?.attributes?.get("fileName")

        val genres = networkManga.attributes.tags.filter {
            it.attributes.group == Group.genre
        }.map {
            it.attributes.name["en"] ?: ""
        }

        val titles = buildMap {
            networkManga.attributes.altTitles.forEach {
                for ((k, v) in it) {
                    put(k, v)
                }
            }
        }

        return Manga(
            id = networkManga.id,
            description = networkManga.attributes.description.getOrDefault("en", ""),
            title = networkManga.attributes.title.getOrDefault("en", ""),
            imageUrl = "https://uploads.mangadex.org/covers/${networkManga.id}/$fileName",
            genres = genres,
            altTitle = networkManga.attributes.altTitles.find { it.containsKey("en") }?.getOrDefault("en", "") ?: "",
            availableTranslatedLanguages = networkManga.attributes.availableTranslatedLanguages.filterNotNull(),
            allDescriptions = networkManga.attributes.description,
            allTitles = titles,
            lastChapter = networkManga.attributes.lastChapter?.toIntOrNull() ?: 0,
            lastVolume = networkManga.attributes.lastVolume?.toIntOrNull() ?: 0,
            status = networkManga.attributes.status,
            year = networkManga.attributes.year ?: 0,
            contentRating = networkManga.attributes.contentRating
        )
    }
}

object SuccessMangaMapper : ApiSuccessModelMapper<MangaListResponse, List<Manga>> {

    private fun toDomain(networkManga: io.silv.amadeus.network.mangadex.models.manga.Manga): Manga {

        val fileName = networkManga.relationships.find {
            it.type == "cover_art"
        }?.attributes?.get("fileName")

        val genres = networkManga.attributes.tags.filter {
            it.attributes.group == Group.genre
        }.map {
            it.attributes.name["en"] ?: ""
        }

        val titles = buildMap {
            networkManga.attributes.altTitles.forEach {
                for ((k, v) in it) {
                    put(k, v)
                }
            }
        }

        return Manga(
            id = networkManga.id,
            description = networkManga.attributes.description.getOrDefault("en", ""),
            title = networkManga.attributes.title.getOrDefault("en", ""),
            imageUrl = "https://uploads.mangadex.org/covers/${networkManga.id}/$fileName",
            genres = genres,
            altTitle = networkManga.attributes.altTitles.find { it.containsKey("en") }?.getOrDefault("en", "") ?: "",
            availableTranslatedLanguages = networkManga.attributes.availableTranslatedLanguages.filterNotNull(),
            allDescriptions = networkManga.attributes.description,
            allTitles = titles,
            lastChapter = networkManga.attributes.lastChapter?.toIntOrNull() ?: 0,
            lastVolume = networkManga.attributes.lastVolume?.toIntOrNull() ?: 0,
            status = networkManga.attributes.status,
            year = networkManga.attributes.year ?: 0,
            contentRating = networkManga.attributes.contentRating
        )
    }

    override fun map(apiSuccessResponse: ApiResponse.Success<MangaListResponse>): List<Manga> {
        return apiSuccessResponse.data.data.map(::toDomain)
    }
}