package io.silv.amadeus

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.core.content.getSystemService
import cafe.adriel.voyager.core.registry.ScreenRegistry
import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.imageDecoderEnabled
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.util.DebugLogger
import eu.kanade.tachiyomi.BufferedSourceFetcher
import eu.kanade.tachiyomi.MangaCoverFactory
import eu.kanade.tachiyomi.MangaCoverKeyer
import eu.kanade.tachiyomi.MangaFactory
import eu.kanade.tachiyomi.MangaKeyer
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder
import io.silv.common.CommonDependencies
import io.silv.common.DependencyAccessor
import io.silv.common.commonDeps
import io.silv.data.download.CoverCache
import io.silv.di.DataDependencies
import io.silv.di.dataDeps
import io.silv.manga.download.DownloadQueueScreen
import io.silv.manga.filter.MangaFilterScreen
import io.silv.manga.view.MangaViewScreen
import io.silv.navigation.SharedScreen
import io.silv.network.NetworkDependencies
import io.silv.network.networkDeps
import io.silv.reader2.ReaderScreen2
import io.silv.sync.Sync
import io.silv.sync.SyncDependencies
import io.silv.sync.syncDependencies
import kotlinx.coroutines.Dispatchers

val debug = true

class AmadeusApp : Application(), SingletonImageLoader.Factory {
    @OptIn(DependencyAccessor::class)
    override fun onCreate() {
        super.onCreate()

        if (debug) AndroidLogcatLogger
            .installOnDebuggableApp(this)

        ScreenRegistry {
            register<SharedScreen.Reader> {
                ReaderScreen2(
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

        commonDeps = object : CommonDependencies() {}

        networkDeps = object : NetworkDependencies() {
            override val context: Context get() = this@AmadeusApp
        }

        dataDeps = object : DataDependencies() {
            override val context: Context get() = this@AmadeusApp
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

    @OptIn(DependencyAccessor::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {

        val client = lazy { networkDeps.mangaDexClient }
        val cache by lazy { CoverCache(this) }

        val diskCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            DiskCache.Builder()
                .directory(cacheDir.also { it.mkdirs() }.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }

        val memCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            MemoryCache.Builder()
                .maxSizePercent(this, 0.25)
                .build()
        }

        return ImageLoader.Builder(this)
            .components(
                ComponentRegistry.Builder()
                    .add(
                        OkHttpNetworkFetcherFactory()
                    )
                    .add(TachiyomiImageDecoder.Factory())
                    .add(BufferedSourceFetcher.Factory())
                    .add(MangaFactory(client, cache))
                    .add(MangaCoverFactory(client, cache))
                    .add(MangaKeyer(cache))
                    .add(MangaCoverKeyer(cache))
                    .build()
            )
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
            .allowRgb565(!isLowRamDevice(this))
            .memoryCache(memCache)
            .diskCache(diskCache)
            .fetcherCoroutineContext (Dispatchers.IO.limitedParallelism(8))
            .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(3))
            .allowHardware(true)
            .imageDecoderEnabled(true)
            .apply {
                if (debug) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
