package io.silv.amadeus.domain

import io.silv.amadeus.domain.repos.MangaRepo
import io.silv.amadeus.network.networkModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {

    includes(networkModule)

    singleOf(::MangaRepo)
}