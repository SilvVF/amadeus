package io.silv.domain.manga

import com.skydoves.sandwich.getOrNull
import io.silv.common.AmadeusDispatchers
import io.silv.common.model.TagsMode
import io.silv.common.time.timeStringMinus
import io.silv.data.manga.MangaRepository
import io.silv.data.mappers.toEntity
import io.silv.model.SavableManga
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GetSavableManga(
    private val mangaRepository: MangaRepository,
    private val mangaDexApi: MangaDexApi,
    private val dispatchers: AmadeusDispatchers,
) {
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

    suspend fun getYearlyTopMangaByTagId(
        tagId: String,
        scope: CoroutineScope = CoroutineScope(dispatchers.io),
        amount: Int = 20,
    ): List<StateFlow<SavableManga>> {
        return (
            mangaDexApi.getMangaList(topYearlyRequest(tagId, amount))
                .getOrNull()
                ?.data ?: emptyList()
            )
            .onEach { mangaRepository.saveManga(it.toEntity()) }
            .map {
                subscribe(it.id)
                    .filterNotNull()
                    .distinctUntilChanged()
                    .stateIn(
                        scope,
                        SharingStarted.Lazily,
                        SavableManga(it.toEntity()),
                    )
            }
    }

    suspend fun await(id: String): SavableManga? {
        return mangaRepository.getMangaById(id)?.let(::SavableManga)
    }

    fun subscribe(id: String): Flow<SavableManga> {
        return mangaRepository.observeMangaById(id).filterNotNull().map { SavableManga(it) }
    }
}
