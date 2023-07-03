package io.silv.manga.sync

import androidx.room.OnConflictStrategy
import androidx.room.Update


internal fun <Type : AmadeusEntity, NetworkType, Key> syncerForEntity(
    entityDao: SyncableDao<Type>,
    networkToKey: (NetworkType) -> Key,
    mapper: (NetworkType, Type?) -> Type,
) = Syncer(
    upsert = entityDao::upsert,
    getIdFromNetwork = networkToKey,
    networkToLocal = mapper,
)

internal data class SyncResult<E: AmadeusEntity>(
    val added: List<E>,
    val updated: List<E>,
    val unhandled: List<E>
)

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

interface AmadeusEntity {
    val id: String
}


interface SyncableDao<in E: AmadeusEntity> {

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: E)
}

suspend fun <E : AmadeusEntity>  SyncableDao<E>.upsert(vararg entities: E) = entities.forEach { e -> upsert(e) }

fun interface Mapper<F, T> {
    fun map(from: F): T
}

fun interface IndexedMapper<F, T> {
    fun map(index: Int, from: F): T
}