package io.silv.domain.manga

import com.skydoves.sandwich.getOrNull
import io.silv.common.AmadeusDispatchers
import io.silv.common.model.TagsMode
import io.silv.common.time.timeStringMinus
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SourceMangaRepository
import io.silv.data.mappers.toSourceManga
import io.silv.model.SavableManga
import io.silv.network.MangaDexApi
import io.silv.network.requests.MangaRequest
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn

class GetSavableManga(
    private val mangaRepository: SavedMangaRepository,
    private val sourceMangaRepository: SourceMangaRepository,
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
            .onEach { sourceMangaRepository.saveManga(it.toSourceManga()) }
            .map {
                subscribe(it.id)
                    .filterNotNull()
                    .distinctUntilChanged()
                    .stateIn(
                        scope,
                        SharingStarted.Lazily,
                        SavableManga(it.toSourceManga()),
                    )
            }
    }

    suspend fun await(id: String): SavableManga? {
        return combine(
            mangaRepository.observeSavedMangaById(id),
            sourceMangaRepository.observeMangaById(id),
        ) { saved, source ->
            saved?.let(::SavableManga) ?: source?.let(::SavableManga)
        }
            .firstOrNull()
    }

    fun subscribe(id: String): Flow<SavableManga> {
        return combine(
            sourceMangaRepository.observeMangaById(id),
            mangaRepository.observeSavedMangaById(id),
        ) { source, saved ->
            saved?.let(::SavableManga) ?: SavableManga(
                source ?: error("no manga found with matching id")
            )
        }
    }
}
