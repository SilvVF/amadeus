package io.silv.data

import io.silv.data.di.authorModule
import io.silv.data.di.chapterModule
import io.silv.data.di.mangaModule
import io.silv.data.di.tagsModule
import io.silv.data.di.workersModule
import io.silv.data.manga.MangaPagingSourceFactory
import io.silv.data.util.UpdateChapterList
import io.silv.database.daosModule
import io.silv.database.databaseModule
import io.silv.network.networkModule
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {

    factoryOf(::UpdateChapterList)

    singleOf(::MangaPagingSourceFactory)

    singleOf(::RecentSearchRepositoryImpl) {
        bind<RecentSearchRepository>()
    }

    includes(
        networkModule,
        databaseModule,
        daosModule,
        authorModule,
        chapterModule,
        mangaModule,
        tagsModule,
        workersModule
    )
}