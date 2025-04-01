package io.silv.amadeus

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import cafe.adriel.voyager.core.registry.ScreenRegistry
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.util.DebugLogger
import eu.kanade.tachiyomi.MangaCoverFetcher
import eu.kanade.tachiyomi.TachiyomiImageDecoder
import io.silv.amadeus.coil.CoilDiskCache
import io.silv.amadeus.coil.CoilMemoryCache
import io.silv.amadeus.coil.addDiskFetcher
import io.silv.amadeus.dependency.CommonDependencies
import io.silv.common.DependencyAccessor
import io.silv.amadeus.dependency.commonDeps
import io.silv.common.AppDependencies
import io.silv.common.appDeps
import io.silv.di.DownloadDependencies
import io.silv.di.downloadDeps
import io.silv.data.download.CoverCache
import io.silv.database.DaosModule
import io.silv.database.DatabaseModule
import io.silv.datastore.DataStoreModule
import io.silv.datastore.DataStoreModuleImpl
import io.silv.datastore.dataStore
import io.silv.datastore.dataStoreDeps
import io.silv.di.DataDependencies
import io.silv.di.dataDeps
import io.silv.manga.download.DownloadQueueScreen
import io.silv.manga.filter.MangaFilterScreen
import io.silv.manga.view.MangaViewScreen
import io.silv.navigation.SharedScreen
import io.silv.network.NetworkDependencies
import io.silv.network.networkDeps
import io.silv.reader.ReaderScreen
import io.silv.sync.Sync
import io.silv.sync.SyncDependencies
import io.silv.sync.syncDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

class AmadeusApp : Application(), ImageLoaderFactory {
    @OptIn(DependencyAccessor::class)
    override fun onCreate() {
        super.onCreate()

        ScreenRegistry {
            register<SharedScreen.Reader> {
                ReaderScreen(
                    mangaId = it.mangaId,
                    chapterId = it.initialChapterId,
                )
            }
            register<SharedScreen.MangaFilter> {
                MangaFilterScreen(it.tag, it.tagId)
            }
            register<SharedScreen.MangaView> {
                MangaViewScreen(it.mangaId)
            }
            register<SharedScreen.DownloadQueue> {
                DownloadQueueScreen()
            }
        }

        appDeps = object : AppDependencies() {}

        val databaseModule = DatabaseModule(this)
        val daosModule = DaosModule(databaseModule)

        dataStoreDeps = DataStoreModuleImpl(this)

        networkDeps = object:  NetworkDependencies() {
            override val context: Context get() = this@AmadeusApp
        }

        dataDeps = object: DataDependencies() {
            override val databaseModule: DatabaseModule = databaseModule
            override val context: Context get() = this@AmadeusApp
            override val daosModule: DaosModule = daosModule
        }

        downloadDeps = object: DownloadDependencies() {
            override val context: Context get() = this@AmadeusApp
            override val dataStoreModule: DataStoreModule get() = dataStoreDeps
        }

        commonDeps = object : CommonDependencies(){
            override val application: Application get() = this@AmadeusApp
        }

        syncDependencies = object : SyncDependencies() {
            override val application: Application get() = this@AmadeusApp
        }

        Sync.init(this)
    }

    private fun isLowRamDevice(context: Context): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        context.getSystemService<ActivityManager>()!!.getMemoryInfo(memInfo)
        val totalMemBytes = memInfo.totalMem
        return totalMemBytes < 3L * 1024 * 1024 * 1024
    }

    @OptIn(ExperimentalCoroutinesApi::class, DependencyAccessor::class)
    override fun newImageLoader(): ImageLoader {

        val client = networkDeps.mangaDexClient
        val cache by lazy { CoverCache(this) }

        val diskCache = { CoilDiskCache.get(this) }
        val memCache = { CoilMemoryCache.get(this) }

        val diskBackedFetcher = MangaCoverFetcher(client, cache, this)

        val builder = ImageLoader.Builder(this)
            .components {
                addDiskFetcher(diskBackedFetcher.coverFetcher, diskCache)
                addDiskFetcher(diskBackedFetcher.mangaFetcher, diskCache)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(TachiyomiImageDecoder.Factory())
            }
            .crossfade(
                (
                        300 *
                                Settings.Global.getFloat(
                                    this@AmadeusApp.contentResolver,
                                    Settings.Global.ANIMATOR_DURATION_SCALE,
                                    1f,
                                )
                        ).toInt(),
            )
            .allowRgb565(isLowRamDevice(this@AmadeusApp))
            .diskCache(diskCache)
            .memoryCache(memCache)
            // Coil spawns a new thread for every image load by default
            .fetcherDispatcher(Dispatchers.IO.limitedParallelism(8))
            .decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
            .transformationDispatcher(Dispatchers.IO.limitedParallelism(2))
        return if (true) {
            // TODO(remove this)
            builder.logger(DebugLogger())
        } else {
            builder
        }
            .build()
    }
}
