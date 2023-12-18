package io.silv.sync

import io.silv.data.dataModule
import io.silv.data.di.workersModule
import io.silv.sync.workers.MangaSyncWorker
import io.silv.sync.workers.SeasonalMangaSyncWorker
import io.silv.sync.workers.TagSyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.named
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val syncModule =
    module {

        includes(dataModule)
        includes(workersModule)

        singleOf(::TagSyncManager) withOptions {
            named(TagSyncWorkName)
            bind<SyncManager>()
        }

        singleOf(::SavedMangaSyncManager) {
            named(MangaSyncWorkName)
            bind<SyncManager>()
        }
        singleOf(::SeasonalMangaSyncManager) {
            named(SeasonalMangaSyncWorkName)
            bind<SyncManager>()
        }

        worker {
            SeasonalMangaSyncWorker(androidContext(), get())
        }

        worker {
            TagSyncWorker(androidContext(), get())
        }

        worker {
            MangaSyncWorker(androidContext(), get())
        }
    }
