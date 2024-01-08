package io.silv.data

import io.silv.common.model.NetworkConnectivity
import io.silv.data.chapter.ChapterRepositoryImpl
import io.silv.data.di.authorModule
import io.silv.data.di.downloadModule
import io.silv.data.di.mangaModule
import io.silv.data.di.tagsModule
import io.silv.data.di.workersModule
import io.silv.data.manga.GetMangaStatisticsById
import io.silv.data.manga.MangaPagingSourceFactoryImpl
import io.silv.data.util.NetworkConnectivityImpl
import io.silv.data.util.UpdateChapterList
import io.silv.database.daosModule
import io.silv.database.databaseModule
import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.domain.search.RecentSearchRepository
import io.silv.network.networkModule
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {

    includes(
        networkModule,
        databaseModule,
        daosModule,
        authorModule,
        mangaModule,
        workersModule,
        tagsModule,
        downloadModule
    )

    singleOf(::ChapterRepositoryImpl) {
        bind<ChapterRepository>()
    }

    singleOf(::NetworkConnectivityImpl) {
        bind<NetworkConnectivity>()
    }

    factoryOf(::UpdateChapterList)

    singleOf(::MangaPagingSourceFactoryImpl)

    factoryOf(::GetMangaStatisticsById)

    singleOf(::RecentSearchRepositoryImpl) {
        bind<RecentSearchRepository>()
    }
}