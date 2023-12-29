package io.silv.data.di

import io.silv.domain.AuthorListRepository
import io.silv.data.author.AuthorListRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authorModule = module {

    singleOf(::AuthorListRepositoryImpl) {
        bind<AuthorListRepository>()
    }
}