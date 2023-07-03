package io.silv.manga.network.mangadex

import ChapterListResponse
import io.silv.core.AmadeusDispatchers
import io.silv.manga.network.mangadex.models.chapter.ChapterResponse
import io.silv.manga.network.mangadex.models.cover.CoverArtListResponse
import io.silv.manga.network.mangadex.models.manga.MangaAggregateResponse
import io.silv.manga.network.mangadex.models.manga.MangaByIdResponse
import io.silv.manga.network.mangadex.models.manga.MangaListResponse
import io.silv.manga.network.mangadex.models.test.MangaDexTestJson
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MangaDexTestApi(
    private val json: Json,
    private val dispatchers: AmadeusDispatchers
) {

    suspend fun getMangaFeed(): ChapterListResponse = withContext(dispatchers.io) {
        json.decodeFromString(
            ChapterListResponse.serializer(),
            MangaDexTestJson.manga_id_feed
        )
    }

    suspend fun getMangaCoverArt(): CoverArtListResponse = withContext(dispatchers.io) {
        json.decodeFromString(
            CoverArtListResponse.serializer(),
            MangaDexTestJson.cover_art_list
        )
    }

    private suspend fun getMangaAggregate(): MangaAggregateResponse =
        withContext(dispatchers.io) { json.decodeFromString(MangaDexTestJson.manga_id_aggregate) }

    suspend fun getMangaById(): MangaByIdResponse =
        withContext(dispatchers.io) { json.decodeFromString(MangaDexTestJson.manga_id) }


    suspend fun getMangaList(): MangaListResponse =
        withContext(dispatchers.io) { json.decodeFromString(MangaDexTestJson.manga) }


    suspend fun getChapterById(): ChapterResponse =
        withContext(dispatchers.io) {  json.decodeFromString(MangaDexTestJson.chapter_id) }

    suspend fun getChapterList(): ChapterListResponse =
        withContext(dispatchers.io) {  json.decodeFromString(MangaDexTestJson.manga_id_feed) }
}