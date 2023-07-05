package io.silv.manga.domain.di


import io.silv.manga.domain.repositorys.ChapterInfoRepository
import io.silv.manga.domain.repositorys.CombinedMangaChapterInfoVolumeImagesRepository
import io.silv.manga.domain.repositorys.CombinedMangaChapterInfoVolumeImagesRepositoryImpl
import io.silv.manga.domain.repositorys.CombinedResourceSavedMangaRepository
import io.silv.manga.domain.repositorys.MangaRepository
import io.silv.manga.domain.repositorys.OfflineFirstChapterInfoRepository
import io.silv.manga.domain.repositorys.OfflineFirstMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val domainModule = module {

    includes(networkModule)

    singleOf(::OfflineFirstMangaRepository) withOptions {
        bind<MangaRepository>()
    }

    singleOf(::CombinedMangaChapterInfoVolumeImagesRepositoryImpl) withOptions {
        bind<CombinedMangaChapterInfoVolumeImagesRepository>()
    }

    singleOf(::CombinedResourceSavedMangaRepository)

    singleOf(::SavedMangaRepositoryImpl) withOptions {
        bind<SavedMangaRepository>()
    }

    singleOf(::OfflineFirstChapterInfoRepository) withOptions {
        bind<ChapterInfoRepository>()
    }
}