package io.silv.manga.repository_usecases

import io.silv.manga.domain.usecase.UpdateChapterList
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val repositoryUseCaseModule = module {


    factory {
        UpdateResourceChapterWithArt.defaultImpl(
            popularMangaResourceDao = get(),
            recentMangaResourceDao = get(),
            searchMangaResourceDao = get(),
            filteredMangaResourceDao = get(),
            seasonalMangaResourceDao = get(),
            filteredMangaYearlyResourceDao = get()
        )
    }

    factory {
        UpdateChapterWithArt.defaultImpl()
    }

    factoryOf(::GetCombinedMangaResources)

    factoryOf(::GetMangaStatisticsById)

    factoryOf(::UpdateChapterList)

    factoryOf(::UpdateMangaResourceWithArt)

    factoryOf(::GetMangaResourcesById)

}

