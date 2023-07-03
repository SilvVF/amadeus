package io.silv.manga.domain.di

import io.silv.manga.domain.repos.MangaRepo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {

    includes(networkModule)

    singleOf(::MangaRepo)
}