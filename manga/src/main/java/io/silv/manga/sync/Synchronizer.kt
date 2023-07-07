package io.silv.manga.sync

import io.silv.manga.local.entity.AmadeusEntity

interface Synchronizer {

    /**
     * Syntactic sugar to call [Syncable.syncWith] while omitting the synchronizer argument
     */
    suspend fun Syncable.sync() = this@sync.syncWith(this@Synchronizer)
}


/**
 * Tries to sync the network and local using the provided [Syncer] and returns true if no
 * exceptions where thrown in the process.
 */
internal suspend fun <Type : AmadeusEntity, NetworkType, Key> Synchronizer.syncWithSyncer(
    syncer: Syncer<Type, NetworkType, Key>,
    getCurrent: suspend () -> List<Type>,
    getNetwork: suspend () -> List<NetworkType>,
    onComplete: suspend (SyncResult<Type>) -> Unit
): Boolean {
    return runCatching {
        val result = syncer.sync(
            current = getCurrent(),
            networkResponse = getNetwork()
        )
        onComplete(result)
    }
        .isSuccess
}
