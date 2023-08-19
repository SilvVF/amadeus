package io.silv.manga


import io.silv.manga.local.daosModule
import io.silv.manga.local.workers.workerModule
import io.silv.manga.network.networkModule
import io.silv.manga.repositorys.repositoryModule
import org.koin.dsl.module

val mangaModule = module {
    includes(
        networkModule,
        workerModule,
        daosModule,
        repositoryModule,
    )
}