package io.silv.common.model

/**
 * Contains the required data for MangaCoverFetcher
 */
data class MangaCover(
    val mangaId: String,
    val url: String,
    val isMangaFavorite: Boolean,
    val lastModified: Long,
)
