package io.silv.amadeus.domain.repos

import com.skydoves.whatif.whatIfNotNullWith
import io.silv.amadeus.domain.mappers.toDomainChapter
import io.silv.amadeus.domain.mappers.toDomainManga
import io.silv.amadeus.domain.models.ChapterImages
import io.silv.amadeus.domain.models.DomainCoverArt
import io.silv.amadeus.domain.models.DomainManga
import io.silv.amadeus.local.dao.ChapterDao
import io.silv.amadeus.local.dao.MangaDao
import io.silv.amadeus.local.dao.VolumeDao
import io.silv.amadeus.network.mangadex.MangaDexApi
import io.silv.amadeus.network.mangadex.requests.CoverArtRequest
import io.silv.amadeus.network.mangadex.requests.MangaFeedRequest
import io.silv.amadeus.network.mangadex.requests.MangaRequest
import io.silv.amadeus.network.mangadex.requests.Order
import io.silv.amadeus.network.mangadex.requests.OrderBy
import io.silv.amadeus.pmap
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.mapSuccess
import io.silv.ktor_response_mapper.onSuccess
import io.silv.ktor_response_mapper.suspendMapSuccess


class MangaRepo(
    private val mangaDexApi: MangaDexApi,
    private val mangaDao: MangaDao,
    private val chapterDao: ChapterDao,
    private val volumeDao: VolumeDao,
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
        translatedLanguage: String = "en",
        limit: Int = 1000
    ) = mangaDexApi.getMangaFeed(
            mangaId = mangaId,
            mangaFeedRequest = MangaFeedRequest(
                order = mapOf(
                    Order.chapter to orderBy
                ),
                translatedLanguage = listOf(translatedLanguage),
                offset = offset,
                limit = limit
            )
        ).mapSuccess {
            data.pmap { networkChapter ->
                val chapter = toDomainChapter(networkChapter)
                chapterDao.getChapterById(networkChapter.id)
                    .whatIfNotNullWith(
                        whatIfNot = { _ -> chapter },
                        whatIf = { entity ->
                            chapter.copy(
                                progress = entity.progressState,
                                downloaded = entity.uris.isNotEmpty(),
                                imageUris = entity.uris
                            )
                        }
                    )
            }
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
            .also { println(it) }
    }

}

