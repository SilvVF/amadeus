package io.silv.data.util

import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.time.epochSeconds
import io.silv.database.entity.AmadeusEntity
import kotlin.time.Duration.Companion.days

/**
 * updates and all local entities with the given network response.
 * @param networkToLocal function to create a entity from network response with the previously saved entity.
 * @param getIdFromNetwork function to convert the network type into a key to find a previously saved entity.
 * @param upsert called after converting the network type into an entity logic to determine whether
 *  to insert to local can happen here.
 *  @property sync function the syncs the local db with the network response returns [SyncResult]
 *  that contains all the inserts, updates, and unhandled being the entities not found in the network response
 *  but contained in the local db. Added and updated assume that [upsert] updated the db.
 */
internal class Syncer<Local : AmadeusEntity<*>, Network, Key>(
    private val networkToLocal: suspend (Network, Local?) -> Local,
    private val getIdFromNetwork: suspend (Network) -> Key,
    private val upsert: suspend (Local) -> Unit,
) {

    /**
     *  function the syncs the local db with the network response returns [SyncResult]
     *  that contains all the inserts, updates, and unhandled being the entities not found in the network response
     *  but contained in the local db. Added and updated assume that [upsert] updated the db.
     */
    suspend fun sync(
        current: List<Local>,
        networkResponse: List<Network>,
    ): SyncResult<Local>  {

        val added = mutableListOf<Local>()
        val updated = mutableListOf<Local>()
        var unhandled = current

        for (networkValue in networkResponse) {

            val savedEntity = current.find { c -> c.id == getIdFromNetwork(networkValue) }

            if (savedEntity != null) {
                val entity = networkToLocal(networkValue, savedEntity)
                upsert(entity)
                unhandled = unhandled.filter { it.id != entity.id }
                updated.add(entity)
            } else {
                val entity = networkToLocal(networkValue, null)
                upsert(entity)
                added.add(entity)
            }
        }

        return SyncResult(
            added = added,
            updated = updated,
            unhandled = unhandled
        )
    }
}

internal suspend fun <Key, Local: AmadeusEntity<Key>, Network> syncVersions(
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

    if (epochSeconds() - lastUpdatedEpochSeconds() > 30.days.inWholeSeconds) {
        val network = getNetworkWithVersion()
        val local = getLocalWithVersions()
        val networkWithKey = network.map { it to networkToKey(it.second) }
        local.forEach { (version, entity) ->
            val networkValue = networkWithKey.find { it.second == entity.id }
            networkValue?.let { (pair, _) ->
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
internal suspend fun <Type : AmadeusEntity<*>, NetworkType, Key> syncUsing(
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


internal fun <Type : AmadeusEntity<*>, NetworkType, Key> createSyncer(
    networkToKey: suspend (NetworkType) -> Key,
    mapper: suspend (NetworkType, Type?) -> Type,
    upsert: suspend (Type) -> Unit
) = Syncer(
    upsert = upsert,
    getIdFromNetwork = networkToKey,
    networkToLocal = mapper,
)