package io.silv.sync

import android.app.Application
import io.silv.common.DependencyAccessor

@DependencyAccessor
lateinit var syncDependencies: SyncDependencies

@OptIn(DependencyAccessor::class)
abstract class SyncDependencies {

    abstract val application: Application

    val tagSyncManager: SyncManager by lazy {
        TagSyncManager(application)
    }

    val seasonalMangaSyncManager: SyncManager by lazy {
        SeasonalMangaSyncManager(application)
    }
}


