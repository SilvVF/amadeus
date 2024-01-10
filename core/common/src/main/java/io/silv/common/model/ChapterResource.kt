package io.silv.common.model

interface ChapterResource {
    val scanlator: String
    val id: String
    val title: String
    val mangaId: String
    val volume: Int
    val chapter: Double
    val url: String
}
