package io.silv.manga.sync

import android.util.Log
import io.silv.manga.local.entity.AmadeusEntity
import kotlinx.datetime.Clock
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException

interface Synchronizer {

    /**
     * Syntactic sugar to call [Syncable.syncWith] while omitting the synchronizer argument
     */
    suspend fun Syncable.sync() = this@sync.syncWith(this@Synchronizer)
}

/**
 * Attempts [block], returning a successful [Result] if it succeeds, otherwise a [Result.Failure]
 * taking care not to break structured concurrency
 */
private suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (exception: Exception) {
    Log.i(
        "suspendRunCatching",
        "Failed to evaluate a suspendRunCatchingBlock. Returning failure Result",
        exception,
    )
    Result.failure(exception)
}


internal suspend fun <Key, Local: AmadeusEntity<Key>, Network> Synchronizer.syncVersions(
    getLocalWithVersions: suspend () -> List<Pair<Int, Local>>,
    getNetworkWithVersion: suspend () -> List<Pair<Int, Network>>,
    networkToKey: (Network) -> Key,
    lastUpdatedEpochSeconds: suspend () -> Long,
    delete: suspend (Local) -> Unit,
    update: suspend (Network, Local) -> Unit,
    insert: suspend (Network) -> Unit
) = suspendRunCatching {

    val toUpdate = mutableListOf<Pair<Network, Local>>()
    val toDelete = mutableListOf<Local>()

    if (Clock.System.now().epochSeconds - lastUpdatedEpochSeconds() > Duration.ofDays(30).seconds) {
       val network = getNetworkWithVersion()
       val local = getLocalWithVersions()
       val networkWithKey = network.map { it to networkToKey(it.second) }
       local.forEach { (version, entity) ->
           val networkValue = networkWithKey.find { it.second == entity.id }
           networkValue?.let { (pair, key) ->
               val (networkVersion, _) = pair
               if (networkVersion > version)  {
                   toUpdate.add(pair.second to entity)
               }
           }
               ?: toDelete.add(entity)
       }
       networkWithKey.forEach {(netWithVersion, key) ->
           if (!local.map { it.second.id }.contains(key)) {
               insert(netWithVersion.second)
           }
       }
    }
    toUpdate.forEach { update(it.first, it.second) }
    toDelete.forEach { delete(it) }
}
    .isSuccess

/**
 * Tries to sync the network and local using the provided [Syncer] and returns true if no
 * exceptions where thrown in the process.
 */
internal suspend fun <Type : AmadeusEntity<Any?>, NetworkType, Key> Synchronizer.syncWithSyncer(
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
