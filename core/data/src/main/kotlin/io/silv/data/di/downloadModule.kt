package io.silv.data.di

import io.silv.data.download.ChapterCache
import io.silv.data.download.DownloadCache
import io.silv.data.download.DownloadManager
import io.silv.data.download.DownloadProvider
import io.silv.data.download.Downloader
import io.silv.data.download.StorageManager
import io.silv.network.sources.ImageSourceFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val downloadModule = module {

    singleOf(::ChapterCache)

    singleOf(::ImageSourceFactory)

    singleOf(::DownloadCache)

    singleOf(::StorageManager)

    singleOf(::DownloadProvider)

    singleOf(::Downloader)

    singleOf(::DownloadManager)
}