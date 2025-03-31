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
import com.skydoves.sandwich.SandwichInitializer
import eu.kanade.tachiyomi.MangaCoverFetcher
import eu.kanade.tachiyomi.TachiyomiImageDecoder
import io.ktor.client.HttpClient
import io.silv.amadeus.coil.CoilDiskCache
import io.silv.amadeus.coil.CoilMemoryCache
import io.silv.amadeus.coil.addDiskFetcher
import io.silv.data.download.CoverCache
import io.silv.manga.download.DownloadQueueScreen
import io.silv.manga.filter.MangaFilterScreen
import io.silv.manga.view.MangaViewScreen
import io.silv.navigation.SharedScreen
import io.silv.network.util.MangaDexApiLogger
import io.silv.reader.ReaderScreen
import io.silv.sync.Sync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import tachiyomi.decoder.BuildConfig

class AmadeusApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        //SandwichInitializer.sandwichOperators += MangaDexApiLogger<Any>()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@AmadeusApp)
            workManagerFactory()
            modules(appModule)
        }

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

        val client by inject<HttpClient>()
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
