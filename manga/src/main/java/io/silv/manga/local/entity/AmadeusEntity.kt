package io.silv.manga.local.entity

import io.silv.manga.sync.Syncer

internal interface AmadeusEntity {
    val id: String
}

internal fun <Type : AmadeusEntity, NetworkType, Key> syncerForEntity(
    networkToKey: suspend (NetworkType) -> Key,
    mapper: suspend (NetworkType, Type?) -> Type,
    upsert: suspend (Type) -> Unit
) = Syncer(
    upsert = upsert,
    getIdFromNetwork = networkToKey,
    networkToLocal = mapper,
)

