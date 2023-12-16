package io.silv.common.model

import kotlinx.datetime.LocalDateTime

interface MangaResource {
    val id: String
    val coverArt: String
    val title: String
    val version: Int
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
}
