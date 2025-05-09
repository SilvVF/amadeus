package io.silv.data.manga

import com.skydoves.sandwich.getOrNull
import io.silv.common.AmadeusDispatchers
import io.silv.common.model.TagsMode
import io.silv.common.time.timeStringMinus
import io.silv.data.manga.interactor.GetManga
import io.silv.data.manga.model.Manga
import io.silv.data.manga.repository.MangaRepository
import io.silv.data.manga.repository.TopYearlyFetcher
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.days

internal class YearlyTopMangaFetcher(
    private val mangaDexApi: MangaDexApi,
    private val mangaRepository: MangaRepository,
    private val getManga: GetManga,
    dispatchers: AmadeusDispatchers,
): TopYearlyFetcher {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private fun topYearlyRequest(
        tagId: String,
        amount: Int,
    ) = MangaRequest(
        offset = 0,
        limit = amount,
        includes = listOf("cover_art"),
        availableTranslatedLanguage = listOf("en"),
        hasAvailableChapters = true,
        order = mapOf("followedCount" to "desc"),
        includedTags = listOf(tagId),
        includedTagsMode = TagsMode.AND,
        createdAtSince = timeStringMinus(365.days),
    )

    override suspend fun getYearlyTopMangaByTagId(
        tagId: String,
        amount: Int,
    ): List<StateFlow<Manga>> {
        return (
                mangaDexApi.getMangaList(topYearlyRequest(tagId, amount))
                    .getOrNull()
                    ?.data
                    ?: emptyList()
        ).also { list ->
              mangaRepository.insertManga(
                  list.map(MangaMapper::dtoToManga)
              )
        }
            .mapNotNull {
                getManga.subscribe(it.id)
                    .filterNotNull()
                    .distinctUntilChanged()
                    .stateIn(
                        scope,
                        SharingStarted.Lazily,
                        getManga.await(it.id) ?: return@mapNotNull null
                    )
            }


    }
}