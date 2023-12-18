package io.silv.network.model.statistics

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsByMangaIdResponse(
    val result: String,
    val statistics: Map<String?, Stats>,
) {
    @Serializable
    data class Stats(
        val comments: Comments? = Comments(),
        val rating: Rating,
        val follows: Int,
    ) {
        @Serializable
        data class Rating(
            val average: Double?,
            val bayesian: Double,
            val distribution: Map<String, Int>,
        )

        @Serializable
        data class Comments(
            val description: String? = null,
            val threadId: Double = 1.0,
            val repliesCount: Double = 0.0,
        )
    }
}
