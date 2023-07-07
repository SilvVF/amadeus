package io.silv.manga.di


import io.silv.manga.domain.repositorys.ChapterInfoRepository
import io.silv.manga.domain.repositorys.CombinedResourceSavedMangaRepository
import io.silv.manga.domain.repositorys.MangaRepository
import io.silv.manga.domain.repositorys.OfflineFirstChapterInfoRepository
import io.silv.manga.domain.repositorys.OfflineFirstMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepositoryImpl
import io.silv.manga.domain.usecase.CombineMangaChapterInfo
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val domainModule = module {

    includes(networkModule)

    singleOf(::OfflineFirstMangaRepository) withOptions {
        bind<MangaRepository>()
    }

    factoryOf(::CombineMangaChapterInfo)

    singleOf(::CombinedResourceSavedMangaRepository)

    singleOf(::SavedMangaRepositoryImpl) withOptions {
        bind<SavedMangaRepository>()
    }

    singleOf(::OfflineFirstChapterInfoRepository) withOptions {
        bind<ChapterInfoRepository>()
    }
}