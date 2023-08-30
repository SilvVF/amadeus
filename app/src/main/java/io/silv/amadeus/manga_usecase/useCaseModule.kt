package io.silv.amadeus.manga_usecase

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {

    factoryOf(::GetCombinedSavableMangaWithChapters)

    factoryOf(::GetSavedMangaWithChaptersList)

    factoryOf(::GetCombinedMangaResources)

    factoryOf(::GetMangaById)

    factoryOf(::GetMangaStatisticsById)
}