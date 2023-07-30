package io.silv.manga.domain.repositorys

import io.silv.manga.domain.timeStringMinus
import io.silv.manga.local.entity.FilteredMangaResource
import io.silv.manga.local.entity.FilteredMangaYearlyResource
import kotlinx.coroutines.flow.Flow
import java.time.Duration

interface FilteredMangaRepository {

    fun getMangaResource(id: String): Flow<FilteredMangaResource?>

    fun getYearlyMangaResource(id: String): Flow<FilteredMangaYearlyResource?>

    fun getYearlyTopResources(
        tag: String
    ): Flow<List<FilteredMangaYearlyResource>>

    fun getMangaResources(
        tagId: String,
        timePeriod: TimePeriod
    ): Flow<List<FilteredMangaResource>>

    suspend fun loadNextPage()

    enum class TimePeriod {
        SixMonths, ThreeMonths, LastMonth, OneWeek, AllTime
    }
}

fun FilteredMangaRepository.TimePeriod.timeString(): String? {
    return timeStringMinus(
            Duration.ofDays(
                when (this) {
                    FilteredMangaRepository.TimePeriod.SixMonths -> 6 * 30
                    FilteredMangaRepository.TimePeriod.ThreeMonths ->  3 * 30
                    FilteredMangaRepository.TimePeriod.LastMonth -> 30
                    FilteredMangaRepository.TimePeriod.OneWeek -> 7
                    else -> return null
                }
            )
        )
}