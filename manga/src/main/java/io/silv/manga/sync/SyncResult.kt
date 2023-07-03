package io.silv.manga.sync

internal data class SyncResult<E: AmadeusEntity>(
    val added: List<E>,
    val updated: List<E>,
    val unhandled: List<E>
)