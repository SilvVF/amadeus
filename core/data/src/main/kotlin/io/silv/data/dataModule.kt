package io.silv.data

import io.silv.common.model.NetworkConnectivity
import io.silv.data.chapter.ChapterRepositoryImpl
import io.silv.data.di.authorModule
import io.silv.data.di.downloadModule
import io.silv.data.di.mangaModule
import io.silv.data.di.tagsModule
import io.silv.data.di.workersModule
import io.silv.data.history.HistoryRepositoryImpl
import io.silv.data.manga.GetMangaCoverArtById
import io.silv.data.manga.GetMangaStatisticsById
import io.silv.data.manga.MangaPagingSourceFactoryImpl
import io.silv.data.updates.MangaUpdateJob
import io.silv.data.updates.UpdatesRepositoryImpl
import io.silv.data.util.GetChapterList
import io.silv.data.util.NetworkConnectivityImpl
import io.silv.database.daosModule
import io.silv.database.databaseModule
import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.domain.history.HistoryRepository
import io.silv.domain.search.RecentSearchRepository
import io.silv.domain.update.UpdatesRepository
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

    singleOf(::UpdatesRepositoryImpl) {
        bind<UpdatesRepository>()
    }

    singleOf(::MangaUpdateJob)

    singleOf(::HistoryRepositoryImpl) {
        bind<HistoryRepository>()
    }

    singleOf(::ChapterRepositoryImpl) {
        bind<ChapterRepository>()
    }

    singleOf(::NetworkConnectivityImpl) {
        bind<NetworkConnectivity>()
    }

    factoryOf(::GetChapterList)

    singleOf(::MangaPagingSourceFactoryImpl)

    factoryOf(::GetMangaStatisticsById)

    factoryOf(::GetMangaCoverArtById)

    singleOf(::RecentSearchRepositoryImpl) {
        bind<RecentSearchRepository>()
    }
}