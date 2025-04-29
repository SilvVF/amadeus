package io.silv.data

/**
 * Interface marker for a class that is synchronized with a remote source.
 */
interface Syncable {
    /**
     * Synchronizes the local database backing the repository with the network.
     * Returns if the sync was successful or not.
     */
    suspend fun sync(): Boolean
}
