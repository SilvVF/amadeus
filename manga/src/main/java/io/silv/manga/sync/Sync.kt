package io.silv.manga.sync


interface Synchronizer {
    /**
     * Syntactic sugar to call [Syncable.syncWith] while omitting the synchronizer argument
     */
    suspend fun Syncable.sync() = this@sync.syncWith(this@Synchronizer)
}

/**
 * Interface marker for a class that is synchronized with a remote source. Syncing must not be
 * performed concurrently and it is the [Synchronizer]'s responsibility to ensure this.
 */
interface Syncable {
    /**
     * Synchronizes the local database backing the repository with the network.
     * Returns if the sync was successful or not.
     */
    suspend fun syncWith(synchronizer: Synchronizer): Boolean
}

internal suspend fun <Type : AmadeusEntity, NetworkType, Key> Synchronizer.syncWithSyncer(
    syncer: Syncer<Type, NetworkType, Key>,
    getCurrent: suspend () -> List<Type>,
    getNetwork: suspend () -> List<NetworkType>,
    onComplete: suspend (SyncResult<Type>) -> Unit
): Boolean {
    return kotlin.runCatching {
        val result = syncer.sync(
            current = getCurrent(),
            networkResponse = getNetwork()
        )
        onComplete(result)
    }
        .isSuccess
}
