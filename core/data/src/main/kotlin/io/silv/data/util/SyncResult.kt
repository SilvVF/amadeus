package io.silv.data.util

internal data class SyncResult<T>(
    val added: List<T>,
    val updated: List<T>,
    val unhandled: List<T>
) {
    val size = added.size + updated.size + unhandled.size
}

