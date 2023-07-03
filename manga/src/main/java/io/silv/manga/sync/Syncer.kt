package io.silv.manga.sync

internal class Syncer<Local : AmadeusEntity, Network, Key>(
    private val networkToLocal: suspend (Network, Local?) -> Local,
    private val getIdFromNetwork: suspend (Network) -> Key,
    private val upsert: suspend (Local) -> Unit,
) {

    suspend fun sync(
        current: List<Local>,
        networkResponse: List<Network>,
    ): SyncResult<Local>  {

        val added = mutableListOf<Local>()
        val updated = mutableListOf<Local>()
        val unhandled = current.toMutableList()

        for (networkValue in networkResponse) {

            val savedEntity = current.find { c -> c.id == getIdFromNetwork(networkValue) }

            if (savedEntity != null) {
                val entity = networkToLocal(networkValue, savedEntity)
                upsert(entity)
                unhandled.remove(entity)
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