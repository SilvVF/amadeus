package io.silv.di

import android.content.Context
import androidx.work.WorkManager
import io.silv.common.AppDependencies
import io.silv.common.DependencyAccessor
import io.silv.common.appDeps
import io.silv.common.model.NetworkConnectivity
import io.silv.data.RecentSearchRepositoryImpl
import io.silv.data.author.AuthorListRepositoryImpl
import io.silv.data.chapter.ChapterRepositoryImpl
import io.silv.data.download.CoverCache
import io.silv.data.history.HistoryRepositoryImpl
import io.silv.data.manga.GetMangaCoverArtById
import io.silv.data.manga.GetMangaStatisticsById
import io.silv.data.manga.MangaPagingSourceFactoryImpl
import io.silv.data.manga.MangaRepositoryImpl
import io.silv.data.manga.SeasonalMangaRepositoryImpl
import io.silv.data.manga.YearlyTopMangaFetcher
import io.silv.data.tags.TagRepositoryImpl
import io.silv.data.updates.MangaUpdateJob
import io.silv.data.updates.UpdatesRepositoryImpl
import io.silv.data.util.GetChapterList
import io.silv.data.util.NetworkConnectivityImpl
import io.silv.database.DaosModule
import io.silv.database.DatabaseModule
import io.silv.datastore.SettingsStore
import io.silv.domain.AuthorListRepository
import io.silv.domain.TagRepository
import io.silv.domain.chapter.interactor.ChapterHandler
import io.silv.domain.chapter.interactor.GetBookmarkedChapters
import io.silv.domain.chapter.interactor.GetChapter
import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.domain.history.GetLibraryLastUpdated
import io.silv.domain.history.HistoryRepository
import io.silv.domain.manga.MangaPagingSourceFactory
import io.silv.domain.manga.SubscribeToPagingData
import io.silv.domain.manga.interactor.GetLibraryMangaWithChapters
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.interactor.GetMangaWithChapters
import io.silv.domain.manga.interactor.MangaHandler
import io.silv.domain.manga.model.MangaUpdate
import io.silv.domain.manga.repository.MangaRepository
import io.silv.domain.manga.repository.SeasonalMangaRepository
import io.silv.domain.manga.repository.TopYearlyFetcher
import io.silv.domain.search.RecentSearchHandler
import io.silv.domain.search.RecentSearchRepository
import io.silv.domain.update.GetUpdateCount
import io.silv.domain.update.UpdatesRepository
import io.silv.network.networkDeps

@DependencyAccessor
lateinit var dataDeps: DataDependencies

@OptIn(DependencyAccessor::class)
abstract class DataDependencies {

    abstract val databaseModule: DatabaseModule
    abstract val daosModule: DaosModule
    abstract val context: Context

    internal val chapterList: GetChapterList
        get() = GetChapterList(
            networkDeps.mangaDexApi,
            appDeps.dispatchers
        )

    val coverCache = CoverCache(context)

    val getManga: GetManga get() = GetManga(mangaRepository)
    val getMangaWithChapters: GetMangaWithChapters get() = GetMangaWithChapters(mangaRepository)
    val getChapter: GetChapter get() = GetChapter(chapterRepository)
    val subscribeToPagingData get() = SubscribeToPagingData(mangaPagingSourceFactory, getManga)
    val mangaHandler get() = MangaHandler(mangaRepository)
    val chapterHandler get() = ChapterHandler(chapterRepository)
    val recentSearchHandler get() = RecentSearchHandler(appDeps.dispatchers, recentSearchRepository)
    val getLibraryLastUpdated get() = GetLibraryLastUpdated(mangaRepository)
    val getBookmarkedChapters get() = GetBookmarkedChapters(chapterRepository)
    val getLibraryMangaWithChapters get() = GetLibraryMangaWithChapters(mangaRepository)
    val getMangaStatisticsById get() = GetMangaStatisticsById(networkDeps.mangaDexApi)
    val getUpdateCount get() = GetUpdateCount(updatesRepository)
    val getMangaCoverArtById get() = GetMangaCoverArtById(networkDeps.mangaDexApi)

    val workManager get() = WorkManager.getInstance(context)

    val connectivity: NetworkConnectivity by lazy {
        NetworkConnectivityImpl(context)
    }

    val settingsStore by lazy {
        SettingsStore(context)
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(daosModule.historyDao, appDeps.dispatchers)
    }

    val tagsRepository: TagRepository by lazy {
        TagRepositoryImpl(daosModule.tagDao, networkDeps.mangaDexApi)
    }


    val updatesRepository: UpdatesRepository by lazy {
        UpdatesRepositoryImpl(daosModule.updatesDao)
    }

    val recentSearchRepository: RecentSearchRepository by lazy {
        RecentSearchRepositoryImpl(daosModule.recentSearchDao)
    }

    val chapterRepository: ChapterRepository by lazy {
        ChapterRepositoryImpl(
            daosModule.chapterDao,
            networkDeps.mangaDexApi,
            chapterList,
            databaseModule.database,
            appDeps.dispatchers
        )
    }
    val seasonalMangaRepository: SeasonalMangaRepository by lazy {
        SeasonalMangaRepositoryImpl(
            networkDeps.mangaDexApi,
            databaseModule.database,
            appDeps.dispatchers,
            mangaRepository
        )
    }
    val topYearlyFetcher: TopYearlyFetcher by lazy {
        YearlyTopMangaFetcher(
            networkDeps.mangaDexApi, mangaRepository, getManga, appDeps.dispatchers
        )
    }
    val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(
            daosModule.tagDao, networkDeps.mangaDexApi
        )
    }
    val authorListRepository: AuthorListRepository by lazy {
        AuthorListRepositoryImpl(networkDeps.mangaDexApi, appDeps.dispatchers)
    }

    val mangaUpdateJob by lazy {
        MangaUpdateJob(
            databaseModule.database,
            daosModule.sourceMangaDao,
            chapterList,
            daosModule.chapterDao
        )
    }

    val mangaRepository: MangaRepository by lazy {
        MangaRepositoryImpl(
            daosModule.sourceMangaDao,
            appDeps.dispatchers,
            databaseModule.database,
            coverCache
        )
    }

    val mangaPagingSourceFactory: MangaPagingSourceFactory by lazy {
        MangaPagingSourceFactoryImpl(mangaRepository, networkDeps.mangaDexApi)
    }
}