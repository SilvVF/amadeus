package io.silv.manga.domain.di


import io.silv.manga.domain.repositorys.MangaRepository
import io.silv.manga.domain.repositorys.OfflineFirstMangaRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val domainModule = module {

    includes(networkModule)

    singleOf(::OfflineFirstMangaRepository) withOptions {
        bind<MangaRepository>()
    }
}