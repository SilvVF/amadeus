package io.silv.manga.sync

import io.silv.manga.local.entity.AmadeusEntity

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
internal class Syncer<Local : AmadeusEntity, Network, Key>(
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