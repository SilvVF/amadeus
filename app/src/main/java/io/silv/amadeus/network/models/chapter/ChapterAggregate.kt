package io.silv.amadeus.network.models.chapter


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterAggregate(
    @SerialName("result")
    val result: String,
    @SerialName("volumes")
    val volumes: List<Volume>
) {

    @Serializable
    data class Volume(
        @SerialName("volume")
        val volume: String,
        @SerialName("count")
        val count: Int,
        @SerialName("chapters")
        val chapters: List<Chapter>
    )  {

        @Serializable
        data class Chapter(
            val chapter: String,
            val id: String,
            val others: List<String>,
            val count: Int
        )
    }
}