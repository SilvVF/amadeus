package io.silv.manga.sync

import io.silv.manga.local.entity.AmadeusEntity

internal data class SyncResult<E: AmadeusEntity<Any?>>(
    val added: List<E>,
    val updated: List<E>,
    val unhandled: List<E>
) {
    val size = added.size + updated.size + unhandled.size
}

