package io.silv.di

import android.content.Context
import io.silv.common.DependencyAccessor
import io.silv.common.commonDeps
import io.silv.data.download.ChapterCache
import io.silv.data.download.DownloadCache
import io.silv.data.download.DownloadManager
import io.silv.data.download.DownloadProvider
import io.silv.data.download.Downloader
import io.silv.datastore.dataStoreDeps
import io.silv.network.networkDeps
import io.silv.network.sources.ImageSourceFactory

@DependencyAccessor
public lateinit var downloadDeps: DownloadDependencies

@OptIn(DependencyAccessor::class)
abstract class DownloadDependencies {

    abstract val context: Context

    val chapterCache = ChapterCache(context, networkDeps.json)

    val imageSourceFactory = ImageSourceFactory(networkDeps.mangaDexClient)

    val downloadProvider = DownloadProvider(context)

    val downloadCache = DownloadCache(
        context,
        downloadProvider
    )

    val downloadManager = DownloadManager(
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
            networkDeps.mangaDexClient,
            dataStoreDeps.downloadStore,
            dataDeps.getChapter,
            dataDeps.getManga,
            commonDeps.dispatchers
        ),
        downloadCache,
        dataDeps.getManga,
        dataDeps.getChapter
    )
}