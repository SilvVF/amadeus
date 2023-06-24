package io.silv.amadeus.network.mangadex

import io.silv.amadeus.AmadeusDispatchers
import io.silv.amadeus.network.mangadex.models.chapter.ChapterListResponse
import io.silv.amadeus.network.mangadex.models.chapter.ChapterResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaAggregateResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaByIdResponse
import io.silv.amadeus.network.mangadex.models.manga.MangaListResponse
import io.silv.amadeus.network.mangadex.models.test.MangaDexTestJson
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MangaDexTestApi(
    private val json: Json,
    private val dispatchers: AmadeusDispatchers
) {

    private suspend fun getMangaAggregate(): MangaAggregateResponse =
        withContext(dispatchers.io) { json.decodeFromString(MangaDexTestJson.manga_id_aggregate) }

    suspend fun getMangaById(): MangaByIdResponse =
        withContext(dispatchers.io) { json.decodeFromString(MangaDexTestJson.manga_id) }


    suspend fun getMangaList(): MangaListResponse =
        withContext(dispatchers.io) { json.decodeFromString(MangaDexTestJson.manga) }


    suspend fun getChapterById(): ChapterResponse =
        withContext(dispatchers.io) {  json.decodeFromString(MangaDexTestJson.chapter_id) }

    suspend fun getChapterList(): ChapterListResponse =
        withContext(dispatchers.io) {  json.decodeFromString(MangaDexTestJson.chapter) }
}