package io.silv.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.work.WorkManager
import io.silv.common.CommonDependencies
import io.silv.common.DependencyAccessor
import io.silv.common.commonDeps
import io.silv.common.model.NetworkConnectivity
import io.silv.data.AuthorListRepository
import io.silv.data.RecentSearchRepositoryImpl
import io.silv.data.TagRepository
import io.silv.data.author.AuthorListRepositoryImpl
import io.silv.data.chapter.ChapterRepositoryImpl
import io.silv.data.chapter.interactor.ChapterHandler
import io.silv.data.chapter.interactor.GetBookmarkedChapters
import io.silv.data.chapter.interactor.GetChapter
import io.silv.data.chapter.interactor.GetNextChapters
import io.silv.data.download.ChapterCache
import io.silv.data.download.CoverCache
import io.silv.data.download.DownloadCache
import io.silv.data.download.DownloadManager
import io.silv.data.download.DownloadProvider
import io.silv.data.download.Downloader
import io.silv.data.history.GetLibraryLastUpdated
import io.silv.data.history.HistoryRepository
import io.silv.data.history.HistoryRepositoryImpl
import io.silv.data.manga.GetMangaCoverArtById
import io.silv.data.manga.GetMangaStatisticsById
import io.silv.data.manga.MangaPagingSourceFactory
import io.silv.data.manga.MangaPagingSourceFactoryImpl
import io.silv.data.manga.MangaRepositoryImpl
import io.silv.data.manga.SeasonalMangaRepositoryImpl
import io.silv.data.manga.SubscribeToPagingData
import io.silv.data.manga.YearlyTopMangaFetcher
import io.silv.data.manga.interactor.GetChaptersByMangaId
import io.silv.data.manga.interactor.GetLibraryMangaWithChapters
import io.silv.data.manga.interactor.GetManga
import io.silv.data.manga.interactor.GetMangaWithChapters
import io.silv.data.manga.interactor.MangaHandler
import io.silv.data.manga.interactor.SetMangaViewerFlags
import io.silv.data.manga.repository.MangaRepository
import io.silv.data.manga.repository.SeasonalMangaRepository
import io.silv.data.manga.repository.TopYearlyFetcher
import io.silv.data.search.RecentSearchHandler
import io.silv.data.search.RecentSearchRepository
import io.silv.data.tags.TagRepositoryImpl
import io.silv.data.update.GetUpdateCount
import io.silv.data.update.UpdatesRepository
import io.silv.data.updates.MangaUpdateJob
import io.silv.data.updates.UpdatesRepositoryImpl
import io.silv.data.util.GetChapterList
import io.silv.data.util.NetworkConnectivityImpl
import io.silv.database.AmadeusDatabase
import io.silv.datastore.DownloadStore
import io.silv.datastore.SettingsStore
import io.silv.datastore.dataStore
import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.network.networkDeps
import io.silv.network.sources.ImageSourceFactory

@DependencyAccessor
lateinit var dataDeps: DataDependencies

@OptIn(DependencyAccessor::class)
abstract class DataDependencies {

    abstract val context: Context


    val db: AmadeusDatabase by lazy {
        Room.databaseBuilder(
            context,
            AmadeusDatabase::class.java,
            "amadeus.db",
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    val chapterDao by lazy { db.chapterDao() }
    val seasonRemoteKeys by lazy { db.seasonalRemoteKeysDao() }
    val seasonalListDao by lazy { db.seasonalListDao() }
    val tagDao by lazy { db.tagDao() }
    val sourceMangaDao by lazy { db.sourceMangaDao() }
    val updatesDao by lazy { db.updatesDao() }
    val recentSearchDao by lazy { db.recentSearchDao() }
    val historyDao by lazy { db.historyDao() }

    val dataStore: DataStore<Preferences> = context.dataStore

    val settingsStore: SettingsStore by lazy {
        SettingsStore(context)
    }

    val downloadStore: DownloadStore by lazy {
        DownloadStore(context)
    }

    internal val chapterList: GetChapterList
        get() = GetChapterList(
            networkDeps.mangaDexApi,
            commonDeps.dispatchers
        )

    val coverCache by lazy { CoverCache(context) }

    val getManga: GetManga get() = GetManga(mangaRepository)
    val getMangaWithChapters: GetMangaWithChapters get() = GetMangaWithChapters(mangaRepository)
    val getChapter: GetChapter get() = GetChapter(chapterRepository)
    val subscribeToPagingData get() = SubscribeToPagingData(mangaPagingSourceFactory, getManga)
    val mangaHandler get() = MangaHandler(mangaRepository, coverCache)
    val chapterHandler get() = ChapterHandler(chapterRepository)
    val recentSearchHandler
        get() = RecentSearchHandler(
            commonDeps.dispatchers,
            recentSearchRepository
        )
    val getLibraryLastUpdated get() = GetLibraryLastUpdated(mangaRepository)
    val getBookmarkedChapters get() = GetBookmarkedChapters(chapterRepository)
    val getLibraryMangaWithChapters get() = GetLibraryMangaWithChapters(mangaRepository)
    val getMangaStatisticsById get() = GetMangaStatisticsById(networkDeps.mangaDexApi)
    val getUpdateCount get() = GetUpdateCount(updatesRepository)
    val getMangaCoverArtById get() = GetMangaCoverArtById(networkDeps.mangaDexApi)
    val getChaptersByMangaId get() = GetChaptersByMangaId(chapterRepository)
    val getNextChapters get() = GetNextChapters(getChaptersByMangaId, getManga)
    val setMangaViewerFlags get() = SetMangaViewerFlags(mangaRepository)

    val chapterCache by lazy { ChapterCache(context, networkDeps.json) }

    val imageSourceFactory by lazy { ImageSourceFactory(networkDeps.mangaDexClient) }

    val downloadProvider by lazy { DownloadProvider(context) }

    val downloadCache by lazy {
        DownloadCache(
            context,
            downloadProvider
        )
    }

    val downloadManager by lazy {
        DownloadManager(
            context,
            commonDeps.applicationScope,
            downloadProvider,
            Downloader(
                context,
                downloadProvider,
                downloadCache,
                chapterCache,
                imageSourceFactory,
                networkDeps.mangaDexApi,
                networkDeps.noCacheClient,
                downloadStore,
                getChapter,
                getManga,
                commonDeps.dispatchers
            ),
            downloadCache,
            getManga,
            getChapter
        )
    }

    val workManager get() = WorkManager.getInstance(context)

    val connectivity: NetworkConnectivity by lazy {
        NetworkConnectivityImpl(context)
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(historyDao, commonDeps.dispatchers)
    }

    val tagsRepository: TagRepository by lazy {
        TagRepositoryImpl(tagDao, networkDeps.mangaDexApi)
    }


    val updatesRepository: UpdatesRepository by lazy {
        UpdatesRepositoryImpl(updatesDao)
    }

    val recentSearchRepository: RecentSearchRepository by lazy {
        RecentSearchRepositoryImpl(recentSearchDao)
    }

    val chapterRepository: ChapterRepository by lazy {
        ChapterRepositoryImpl(
            chapterDao,
            networkDeps.mangaDexApi,
            chapterList,
            db,
            commonDeps.dispatchers
        )
    }
    val seasonalMangaRepository: SeasonalMangaRepository by lazy {
        SeasonalMangaRepositoryImpl(
            networkDeps.mangaDexApi,
            db,
            commonDeps.dispatchers,
            mangaRepository
        )
    }
    val topYearlyFetcher: TopYearlyFetcher by lazy {
        YearlyTopMangaFetcher(
            networkDeps.mangaDexApi, mangaRepository, getManga, commonDeps.dispatchers
        )
    }
    val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(
            tagDao, networkDeps.mangaDexApi
        )
    }
    val authorListRepository: AuthorListRepository by lazy {
        AuthorListRepositoryImpl(networkDeps.mangaDexApi, commonDeps.dispatchers)
    }

    val mangaUpdateJob by lazy {
        MangaUpdateJob(
            db,
            sourceMangaDao,
            chapterList,
            chapterDao
        )
    }

    val mangaRepository: MangaRepository by lazy {
        MangaRepositoryImpl(
            sourceMangaDao,
            commonDeps.dispatchers,
            db,
            coverCache
        )
    }

    val mangaPagingSourceFactory: MangaPagingSourceFactory by lazy {
        MangaPagingSourceFactoryImpl(networkDeps.mangaDexApi, mangaRepository)
    }
}