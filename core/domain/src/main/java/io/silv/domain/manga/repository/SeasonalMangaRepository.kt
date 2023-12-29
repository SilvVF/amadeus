package io.silv.domain.manga.repository

import io.silv.domain.Syncable
import io.silv.model.DomainSeasonalList
import kotlinx.coroutines.flow.Flow


interface SeasonalMangaRepository: Syncable {

    fun subscribe(): Flow<List<DomainSeasonalList>>
}
