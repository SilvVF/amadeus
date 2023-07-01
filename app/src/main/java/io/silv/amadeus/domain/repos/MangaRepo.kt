package io.silv.amadeus.domain.repos

import io.silv.amadeus.domain.models.ChapterImages
import io.silv.amadeus.domain.models.DomainChapter
import io.silv.amadeus.domain.models.DomainCoverArt
import io.silv.amadeus.domain.models.DomainManga
import io.silv.amadeus.network.mangadex.MangaDexApi
import io.silv.amadeus.network.mangadex.models.Group
import io.silv.amadeus.network.mangadex.models.chapter.Chapter
import io.silv.amadeus.network.mangadex.requests.CoverArtRequest
import io.silv.amadeus.network.mangadex.requests.MangaFeedRequest
import io.silv.amadeus.network.mangadex.requests.MangaRequest
import io.silv.amadeus.network.mangadex.requests.Order
import io.silv.amadeus.network.mangadex.requests.OrderBy
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.mapSuccess
import io.silv.ktor_response_mapper.onSuccess
import io.silv.ktor_response_mapper.suspendMapSuccess


class MangaRepo(
    private val mangaDexApi: MangaDexApi,
) {

    private var mangaListOffset: Int = 0

    suspend fun getChapterImages(
        chapterId: String
    ): ApiResponse<ChapterImages>  {
       return mangaDexApi.getChapterImages(chapterId).suspendMapSuccess {
           ChapterImages(
               images = chapter.data.map { imgFile ->
                   ChapterImages.Image("${baseUrl}/data/${chapter.hash}/$imgFile")
               },
               dataSaverImages = chapter.dataSaver.map { imgFile ->
                   ChapterImages.Image("${baseUrl}/dataSaver/${chapter.hash}/$imgFile")
               }
           )
       }
    }

    suspend fun getVolumeImages(
        mangaId: String,
        limit: Int,
        offset: Int
    ) = mangaDexApi.getCoverArtList(
            CoverArtRequest(
                limit = limit,
                offset = offset,
                manga = listOf(mangaId),
                order = mapOf(Order.volume to OrderBy.asc)
            )
    ).mapSuccess {
        data.map {
            DomainCoverArt(
                volume = it.attributes.volume,
                mangaId = mangaId,
                coverArtUrl = "https://uploads.mangadex.org/covers/$mangaId/${it.attributes.fileName}"
            )
        }
    }

    suspend fun getMangaFeed(
        mangaId: String,
        offset: Int = 0,
        orderBy: OrderBy = OrderBy.asc,
        translatedLanguage: String = "en"
    ) = mangaDexApi.getMangaFeed(
            mangaId = mangaId,
            mangaFeedRequest = MangaFeedRequest(
                order = mapOf(
                    Order.chapter to orderBy
                ),
                translatedLanguage = listOf(translatedLanguage),
                offset = offset
            )
        ).mapSuccess {
            data.map(::toDomainChapter)
        }

    suspend fun getMangaWithArt(amount: Int = 50): ApiResponse<List<DomainManga>> {
        return mangaDexApi.getMangaList(
                MangaRequest(
                    limit = amount,
                    offset = mangaListOffset,
                    includes = listOf("cover_art")
                )
            )
            .mapSuccess {
                data.map(::toDomainManga)
            }
            .onSuccess { mangaListOffset += amount }
            .also {
                println(it)
            }
    }


    private fun toDomainManga(networkManga: io.silv.amadeus.network.mangadex.models.manga.Manga): DomainManga {

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

        return DomainManga(
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
            contentRating = networkManga.attributes.contentRating,
        )
    }
}

fun toDomainChapter(chapter: Chapter): DomainChapter {
    val it = chapter
    return DomainChapter(
        title = it.attributes.title,
        volume = it.attributes.volume,
        chapter = it.attributes.chapter,
        pages = it.attributes.pages,
        translatedLanguage = it.attributes.translatedLanguage,
        updatedAt = it.attributes.updatedAt,
        uploader = it.attributes.uploader ?: "",
        externalUrl = it.attributes.externalUrl,
        version = it.attributes.version,
        createdAt = it.attributes.createdAt,
        readableAt = it.attributes.readableAt,
        mangaId = it.attributes.relationships.find {
            it.type == "manga"
        }?.id ?: ""
    )
}