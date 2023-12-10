package io.silv.data.di

import io.silv.data.tags.TagRepository
import io.silv.data.tags.TagRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val tagsModule = module {

    singleOf(::TagRepositoryImpl) {
        bind<TagRepository>()
    }
}