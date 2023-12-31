package io.silv.data.util

import io.silv.database.entity.manga.MangaEntity
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.model.MangaUpdate

/**
 * Call before updating [Manga.coverArt] to ensure old cover can be cleared from cache
 */
internal fun MangaEntity.deleteOldCoverFromCache(
    coverCache: CoverCache,
    update: MangaUpdate,
    refreshSameUrl: Boolean = false
): MangaUpdate {
    // Never refresh covers if the new url is null, as the current url has possibly become invalid
    val newUrl = update.coverArt
        .ifEmpty { null }
        ?: return update.copy(coverArt = coverArt)

    if (!refreshSameUrl && coverArt == newUrl) return update

    coverCache.deleteFromCache(this, false)

    return update
}
