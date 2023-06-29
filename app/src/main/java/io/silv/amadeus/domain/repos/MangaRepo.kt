package io.silv.amadeus.domain.repos

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.silv.amadeus.domain.models.ChapterImages
import io.silv.amadeus.domain.models.Manga
import io.silv.amadeus.domain.models.Volume
import io.silv.amadeus.local.workers.ChapterDownloadWorker
import io.silv.amadeus.network.mangadex.MangaDexApi
import io.silv.amadeus.network.mangadex.models.Group
import io.silv.amadeus.network.mangadex.models.chapter.ChapterImageResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaAggregateResponse
import io.silv.amadeus.network.mangadex.requests.MangaRequest
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.ApiSuccessModelMapper
import io.silv.ktor_response_mapper.mapSuccess
import io.silv.ktor_response_mapper.onSuccess
import io.silv.ktor_response_mapper.suspendOnSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID


class MangaRepo(
    private val mangaDexApi: MangaDexApi,
    private val context: Context
) {

    private var mangaListOffset: Int = 0

    suspend fun getChapterImages(
        mangaId: String,
        volume: Int,
        chapter: Int,
        lifecycleOwner: LifecycleOwner
    ): Flow<ChapterImages> = channelFlow {
       getMangaChapters(mangaId).suspendOnSuccess {

           val mangaResponse = this

           val chapterId = mangaResponse.data.random().chapters.random().id

           mangaDexApi.getChapterImages(chapterId)
               .suspendOnSuccess(MangaImageSuccessMapper) {
                   send(this)

                   val workId = UUID.randomUUID()
                   val workRequest = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                       .setId(workId)
                       .setInputData(
                           Data.Builder()
                               .putStringArray(ChapterDownloadWorker.imageUrlsKey, images.map { it.uri }.toTypedArray())
                               .putString(ChapterDownloadWorker.chapterIdKey, chapterId)
                               .putString(ChapterDownloadWorker.mangaIdKey, mangaId)
                               .putString(ChapterDownloadWorker.volumeNumberKey, volume.toString())
                               .build()
                       )
                       .build()

                   val worker = WorkManager.getInstance(context)

                   worker.enqueue(workRequest)

                   withContext(Dispatchers.Main) {
                       worker.getWorkInfoByIdLiveData(workId)
                           .observe(lifecycleOwner) { info ->
                               info.outputData.getStringArray("uris")?.let { uris ->
                                   println("urisssssss ${uris.toList()}")
                                   trySend(
                                       ChapterImages(
                                           images = uris.toList().map { ChapterImages.Image(it) },
                                           dataSaverImages = emptyList()
                                       )
                                   )
                               }
                           }
                   }
               }
           awaitClose()
        }
    }
        .flowOn(Dispatchers.IO)

    private object MangaImageSuccessMapper: ApiSuccessModelMapper<ChapterImageResponse, ChapterImages> {
        override fun map(apiSuccessResponse: ApiResponse.Success<ChapterImageResponse>): ChapterImages {
            val data = apiSuccessResponse.data
            return ChapterImages(
                images = data.chapter.data.map { imgFile ->
                    ChapterImages.Image("${data.baseUrl}/data/${data.chapter.hash}/$imgFile")
                },
                dataSaverImages = data.chapter.dataSaver.map { imgFile ->
                    ChapterImages.Image("${data.baseUrl}/dataSaver/${data.chapter.hash}/$imgFile")
                }
            )
        }
    }

    suspend fun getMangaChapters(mangaId: String): ApiResponse<List<Volume>> {
        return mangaDexApi.getMangaAggregate(mangaId)
            .mapSuccess {
                volumes.map(::toDomainVolume)
            }
    }

    suspend fun getMangaWithArt(amount: Int = 50): ApiResponse<List<Manga>> {
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


    private fun toDomainVolume(volume: Map.Entry<String, MangaAggregateResponse.MangaAggregateData>): Volume {
        return Volume(
            number = volume.value.volume.toIntOrNull() ?: 0,
            count = volume.value.count,
            chapters = volume.value.chapters.values.map {
                Volume.Chapter(
                    id = it.id,
                    others = it.others
                )
            }
        )
    }

    private fun toDomainManga(networkManga: io.silv.amadeus.network.mangadex.models.manga.Manga): Manga {

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
            contentRating = networkManga.attributes.contentRating,
        )
    }

    private fun List<Volume>.chapterIdOrNull(
        volume: Int,
        chapter: Int,
    ) = this.getOrNull(volume)?.chapters?.getOrNull(chapter)?.id

}