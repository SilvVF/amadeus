package io.silv.data.util

import io.silv.data.download.CoverCache
import io.silv.database.entity.manga.MangaEntity
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.MangaUpdate

/**
 * Call before updating [Manga.coverArt] to ensure old cover can be cleared from cache
 */
internal fun MangaEntity.deleteOldCoverFromCache(
    coverCache: CoverCache,
    update: MangaUpdate,
    refreshSameUrl: Boolean = false
): Boolean {
    return kotlin.runCatching {
        val newUrl = update.coverArt
            ?.ifEmpty { null }
            ?: return false

        if (!refreshSameUrl && coverArt == newUrl) return false

        if (favorite) {
            return coverCache.deleteFromCache(this, false) > 0
        }
        return false

    }.getOrDefault(false)
}
