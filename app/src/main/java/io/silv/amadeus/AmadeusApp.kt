package io.silv.amadeus

import android.app.Application
import io.silv.amadeus.network.networkModule
import io.silv.amadeus.ui.screens.screenModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AmadeusApp: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@AmadeusApp)
            modules(appModule, networkModule, screenModule)
        }
    }
}