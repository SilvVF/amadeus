package io.silv

import io.silv.data.download.DownloadWorker
import io.silv.domain.chapter.ChapterHandler
import io.silv.domain.chapter.GetSavableChapter
import io.silv.domain.manga.GetLibraryMangaWithChapters
import io.silv.domain.manga.GetMangaStatisticsById
import io.silv.domain.manga.GetSavableManga
import io.silv.domain.manga.GetSavableMangaWithChapters
import io.silv.domain.manga.MangaHandler
import io.silv.domain.manga.SubscribeToPagingData
import io.silv.domain.manga.SubscribeToSeasonalLists
import io.silv.domain.search.RecentSearchHandler
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule =
    module {

        factoryOf(::GetSavableMangaWithChapters)

        factoryOf(::GetLibraryMangaWithChapters)

        factoryOf(::GetMangaStatisticsById)

        factoryOf(::SubscribeToPagingData)

        factoryOf(::SubscribeToSeasonalLists)

        factoryOf(::RecentSearchHandler)

        workerOf(::DownloadWorker)

        factoryOf(::GetSavableManga)

        factoryOf(::GetSavableChapter)

        factoryOf(::MangaHandler)

        factoryOf(::ChapterHandler)
    }
