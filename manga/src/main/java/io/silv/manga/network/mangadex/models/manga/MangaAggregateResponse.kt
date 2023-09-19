package io.silv.manga.network.mangadex.models.manga

import kotlinx.serialization.Serializable

//@Serializable
//data class MangaAggregateResponse(
//    val result: String,
//    val volumes: Map<String, Volume>
//) {
//
//    @Serializable
//    data class Volume(
//        val volume: String = "-1",
//        val count: Int = 0,
//        val chapters: Map<String, Chapter>
//    ) {
//
//        @Serializable
//        data class Chapter(
//            val chapter: String,
//            val id: String,
//            val others: List<String>,
//            val count: Int = 0
//        )
//    }
//}