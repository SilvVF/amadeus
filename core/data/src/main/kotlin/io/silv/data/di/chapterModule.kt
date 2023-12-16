package io.silv.data.di

import io.silv.data.chapter.ChapterRepository
import io.silv.data.chapter.ChapterRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val chapterModule = module {

    singleOf(::ChapterRepositoryImpl) withOptions {
        bind<ChapterRepository>()
    }
}