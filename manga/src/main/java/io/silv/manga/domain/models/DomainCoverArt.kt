package io.silv.manga.domain.models

data class DomainCoverArt(
    val volume: String?,
    val mangaId: String,
    val coverArtUrl: String
)