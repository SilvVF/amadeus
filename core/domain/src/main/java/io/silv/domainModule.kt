package io.silv

import eu.kanade.tachiyomi.DownloadJob
import eu.kanade.tachiyomi.DownloadStore
import eu.kanade.tachiyomi.Downloader
import eu.kanade.tachiyomi.reader.DownloadManager
import eu.kanade.tachiyomi.reader.DownloadProvider
import eu.kanade.tachiyomi.reader.StorageManager
import eu.kanade.tachiyomi.reader.cache.ChapterCache
import eu.kanade.tachiyomi.reader.cache.DownloadCache
import io.silv.domain.chapter.ChapterHandler
import io.silv.domain.chapter.GetChapter
import io.silv.domain.manga.GetCombinedSavableMangaWithChapters
import io.silv.domain.manga.GetManga
import io.silv.domain.manga.GetMangaStatisticsById
import io.silv.domain.manga.GetSavedMangaWithChaptersList
import io.silv.domain.manga.MangaHandler
import io.silv.domain.manga.SubscribeToPagingData
import io.silv.domain.manga.SubscribeToSeasonalLists
import io.silv.domain.search.RecentSearchHandler
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {

    singleOf(::StorageManager)

    singleOf(::ChapterCache)

    factoryOf(::GetCombinedSavableMangaWithChapters)

    factoryOf(::GetSavedMangaWithChaptersList)

    factoryOf(::GetMangaStatisticsById)

    factoryOf(::SubscribeToPagingData)

    factoryOf(::SubscribeToSeasonalLists)

    factoryOf(::RecentSearchHandler)

    factoryOf(::DownloadProvider)

    singleOf(::Downloader)

    singleOf(::DownloadCache)

    workerOf(::DownloadJob)

    singleOf(::DownloadStore)

    singleOf(::DownloadManager)

    factoryOf(::GetManga)

    factoryOf(::GetChapter)

    factoryOf(::MangaHandler)

    factoryOf(::ChapterHandler)
}