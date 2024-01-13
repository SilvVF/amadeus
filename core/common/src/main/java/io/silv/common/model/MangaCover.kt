package io.silv.common.model

import androidx.compose.runtime.Stable

/**
 * Contains the required data for MangaCoverFetcher
 */
@Stable
data class MangaCover(
    val mangaId: String,
    val url: String,
    val isMangaFavorite: Boolean,
    val lastModified: Long,
)
