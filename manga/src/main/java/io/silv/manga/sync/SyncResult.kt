package io.silv.manga.sync

import io.silv.manga.local.entity.AmadeusEntity

internal data class SyncResult<E: AmadeusEntity>(
    val added: List<E>,
    val updated: List<E>,
    val unhandled: List<E>
)