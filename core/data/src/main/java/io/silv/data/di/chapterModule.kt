package io.silv.data.di

import io.silv.data.chapter.ChapterEntityRepository
import io.silv.data.chapter.ChapterImageRepository
import io.silv.data.chapter.ChapterImageRepositoryImpl
import io.silv.data.chapter.OfflineFirstChapterInfoRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val chapterModule = module {

    singleOf(::ChapterImageRepositoryImpl) withOptions {
        bind<ChapterImageRepository>()
    }

    singleOf(::OfflineFirstChapterInfoRepository) withOptions {
        bind<ChapterEntityRepository>()
    }
}