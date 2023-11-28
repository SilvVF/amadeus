package io.silv.amadeus.manga_usecase

import io.silv.ktor_response_mapper.mapSuccess
import io.silv.network.MangaDexApi
import kotlin.math.roundToInt


data class MangaStats(
    val follows: Int = 0,
    val rating: Double = 0.0,
    val comments: Int = 0
)

/**
 * Fetches the [MangaStats] for the given manga id from the Manga Dex api.
 * This always fetches from the network.
 */
class GetMangaStatisticsById (
    private val mangaDexApi: io.silv.network.MangaDexApi
) {
    suspend operator fun invoke(id: String) = mangaDexApi.getMangaStatistics(id)
        .mapSuccess {
            val stats = statistics.values.toList().firstOrNull()
            MangaStats(
                follows = stats?.follows ?: -1,
                rating = stats?.rating?.average ?: -1.0,
                comments = stats?.comments?.repliesCount?.roundToInt() ?: 0
            )
        }
}