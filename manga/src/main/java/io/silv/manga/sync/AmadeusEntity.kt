package io.silv.manga.sync

internal interface AmadeusEntity {
    val id: String
}

internal fun <Type : AmadeusEntity, NetworkType, Key> syncerForEntity(
    networkToKey: (NetworkType) -> Key,
    mapper: (NetworkType, Type?) -> Type,
    upsert: suspend (Type) -> Unit
) = Syncer(
    upsert = upsert,
    getIdFromNetwork = networkToKey,
    networkToLocal = mapper,
)

