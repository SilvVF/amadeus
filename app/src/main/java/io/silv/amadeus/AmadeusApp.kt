package io.silv.amadeus

import android.app.Application
import cafe.adriel.voyager.core.registry.ScreenRegistry
import io.silv.explore.ExploreScreen
import io.silv.ktor_response_mapper.KSandwichInitializer
import io.silv.navigation.SharedScreen
import io.silv.network.util.MangaDexApiLogger
import io.silv.sync.Sync
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AmadeusApp: Application() {

    override fun onCreate() {
        super.onCreate()

        KSandwichInitializer.sandwichOperators += MangaDexApiLogger<Any>()

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
        }

        Sync.init(this)
    }
}