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
import coil.disk.DiskCache
import coil.util.DebugLogger
import com.skydoves.sandwich.SandwichInitializer
import eu.kanade.tachiyomi.CoverCache
import eu.kanade.tachiyomi.MangaCoverFetcher
import eu.kanade.tachiyomi.MangaCoverKeyer
import eu.kanade.tachiyomi.MangaKeyer
import eu.kanade.tachiyomi.TachiyomiImageDecoder
import io.silv.explore.ExploreScreen
import io.silv.library.LibraryScreen
import io.silv.manga.manga_filter.MangaFilterScreen
import io.silv.manga.manga_view.MangaViewScreen
import io.silv.navigation.SharedScreen
import io.silv.network.util.MangaDexApiLogger
import io.silv.reader.ReaderScreen
import io.silv.sync.Sync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AmadeusApp: Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        SandwichInitializer.sandwichOperators += MangaDexApiLogger<Any>()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@AmadeusApp)
            workManagerFactory()
            modules(appModule)
        }


        ScreenRegistry {
            register<SharedScreen.Explore> {
                ExploreScreen()
            }
            register<SharedScreen.Library> {
                LibraryScreen()
            }
            register<SharedScreen.Reader> {
                ReaderScreen(
                    mangaId = it.mangaId,
                    chapterId = it.initialChapterId
                )
            }
            register<SharedScreen.MangaFilter> {
                MangaFilterScreen(it.tag, it.tagId)
            }
            register<SharedScreen.MangaView> {
                MangaViewScreen(it.mangaId)
            }
        }

       Sync.init(this)
    }

    private fun isLowRamDevice(context: Context): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        context.getSystemService<ActivityManager>()!!.getMemoryInfo(memInfo)
        val totalMemBytes = memInfo.totalMem
        return totalMemBytes < 3L * 1024 * 1024 * 1024
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this).apply {

            val callFactoryInit = inject<OkHttpClient>()
            val diskCacheInit = { CoilDiskCache.get(this@AmadeusApp) }

            components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(TachiyomiImageDecoder.Factory())
                add(MangaCoverFetcher.MangaFactory(callFactoryInit, lazy(diskCacheInit)))
                add(MangaCoverFetcher.MangaCoverFactory(callFactoryInit, lazy(diskCacheInit)))
                add(MangaKeyer())
                add(MangaCoverKeyer(coverCache = inject<CoverCache>().value))
            }
            callFactory { callFactoryInit.value }
            diskCache(diskCacheInit)
            crossfade(
                (300 * Settings.Global.getFloat(
                    this@AmadeusApp.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f)
                        ).toInt()
            )
            allowRgb565(isLowRamDevice(this@AmadeusApp))
            logger(DebugLogger())

            // Coil spawns a new thread for every image load by default
            fetcherDispatcher(Dispatchers.IO.limitedParallelism(8))
            decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
            transformationDispatcher(Dispatchers.IO.limitedParallelism(2))
        }.build()
    }
}

/**
 * Direct copy of Coil's internal SingletonDiskCache so that [MangaCoverFetcher] can access it.
 */
private object CoilDiskCache {

    private const val FOLDER_NAME = "image_cache"
    private var instance: DiskCache? = null

    @Synchronized
    fun get(context: Context): DiskCache {
        return instance ?: run {
            val safeCacheDir = context.cacheDir.apply { mkdirs() }
            // Create the singleton disk cache instance.
            DiskCache.Builder()
                .directory(safeCacheDir.resolve(FOLDER_NAME))
                .build()
                .also { instance = it }
        }
    }
}