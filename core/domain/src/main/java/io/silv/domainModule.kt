package io.silv

import io.silv.domain.CombineSourceMangaWithSaved
import io.silv.domain.GetCombinedSavableMangaWithChapters
import io.silv.domain.GetMangaStatisticsById
import io.silv.domain.GetSavedMangaWithChaptersList
import io.silv.domain.RecentSearchHandler
import io.silv.domain.SubscribeToPagingData
import io.silv.domain.SubscribeToSeasonalLists
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {

    factoryOf(::GetCombinedSavableMangaWithChapters)

    factoryOf(::GetSavedMangaWithChaptersList)

    factoryOf(::GetMangaStatisticsById)

    factoryOf(::CombineSourceMangaWithSaved)

    factoryOf(::SubscribeToPagingData)

    factoryOf(::SubscribeToSeasonalLists)

    factoryOf(::RecentSearchHandler)
}