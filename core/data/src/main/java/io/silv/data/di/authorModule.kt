package io.silv.data.di

import io.silv.data.author.AuthorListRepository
import io.silv.data.author.AuthorListRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val authorModule = module {

    singleOf(::AuthorListRepositoryImpl) withOptions {
        bind<AuthorListRepository>()
    }
}