package io.silv.data.manga.repository

import io.silv.data.Syncable
import io.silv.model.DomainSeasonalList
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository: Syncable {

    fun subscribe(): Flow<List<DomainSeasonalList>>
}
