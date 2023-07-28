package io.silv.manga.domain.repositorys

import io.silv.manga.domain.usecase.useCaseModule
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val repositoryModule = module {

    includes(useCaseModule)

    singleOf(::RecentMangaRepositoryImpl) withOptions {
        bind<RecentMangaRepository>()
    }

    singleOf(::PopularMangaRepositoryImpl) withOptions {
        bind<PopularMangaRepository>()
    }

    singleOf(::SearchMangaRepositoryImpl) withOptions {
        bind<SearchMangaRepository>()
    }

    singleOf(::SavedMangaRepositoryImpl) withOptions {
        bind<SavedMangaRepository>()
    }

    singleOf(::OfflineFirstChapterInfoRepository) withOptions {
        bind<ChapterInfoRepository>()
    }

    singleOf(::SeasonalMangaRepositoryImpl) withOptions {
        bind<SeasonalMangaRepository>()
    }

    singleOf(::FilteredMangaRepositoryImpl) withOptions {
        bind<FilteredMangaRepository>()
    }
}