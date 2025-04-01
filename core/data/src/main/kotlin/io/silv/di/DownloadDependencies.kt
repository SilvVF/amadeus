package io.silv.di

import android.content.Context
import io.silv.common.DependencyAccessor
import io.silv.common.appDeps
import io.silv.data.download.ChapterCache
import io.silv.data.download.DownloadCache
import io.silv.data.download.DownloadManager
import io.silv.data.download.DownloadProvider
import io.silv.data.download.Downloader
import io.silv.data.download.StorageManager
import io.silv.datastore.DataStoreModule
import io.silv.network.networkDeps
import io.silv.network.sources.ImageSourceFactory

@DependencyAccessor
public lateinit var downloadDeps: DownloadDependencies

@OptIn(DependencyAccessor::class)
abstract class DownloadDependencies {

    abstract val context: Context
    abstract val dataStoreModule: DataStoreModule

    val chapterCache = ChapterCache(context, networkDeps.json)

    val imageSourceFactory = ImageSourceFactory(networkDeps.mangaDexClient)

    private val storageManager = StorageManager(context)
    private val downloadProvider = DownloadProvider(storageManager)

    val downloadCache = DownloadCache(
        context,
        downloadProvider,
        storageManager
    )

    val downloadManager = DownloadManager(
        context,
        appDeps.applicationScope,
        downloadProvider,
        Downloader(
            context,
            downloadProvider,
            downloadCache,
            chapterCache,
            imageSourceFactory,
            networkDeps.mangaDexApi,
            networkDeps.mangaDexClient,
            dataStoreModule.downloadStore,
            dataDeps.getChapter,
            dataDeps.getManga,
            appDeps.applicationScope
        ),
        downloadCache,
        dataDeps.getManga,
        dataDeps.getChapter
    )
}