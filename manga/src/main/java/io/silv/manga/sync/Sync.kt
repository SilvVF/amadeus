package io.silv.manga.sync

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier

object Sync: KoinComponent {

    private val syncManger by inject<SyncManager>(qualifier("Manga"))

    fun init() {
        syncManger.requestSync()
    }
}

