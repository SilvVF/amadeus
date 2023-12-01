package io.silv

import io.silv.domain.CombineSourceMangaWithSaved
import io.silv.domain.GetCombinedMangaResources
import io.silv.domain.GetCombinedSavableMangaWithChapters
import io.silv.domain.GetMangaStatisticsById
import io.silv.domain.GetQueryPagingData
import io.silv.domain.GetSavedMangaWithChaptersList
import io.silv.domain.SubscribeToPagingData
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {

    factoryOf(::GetCombinedSavableMangaWithChapters)

    factoryOf(::GetSavedMangaWithChaptersList)

    factoryOf(::GetCombinedMangaResources)

    factoryOf(::GetMangaStatisticsById)

    factoryOf(::CombineSourceMangaWithSaved)

    factoryOf(::GetQueryPagingData)

    factoryOf(::SubscribeToPagingData)
}