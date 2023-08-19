package io.silv.manga.repository_usecases

import io.silv.ktor_response_mapper.mapSuccess
import io.silv.manga.network.mangadex.MangaDexApi
import kotlin.math.roundToInt


data class MangaStats(
    val follows: Int = 0,
    val rating: Double = 0.0,
    val comments: Int = 0
)

class GetMangaStatisticsById (
    private val mangaDexApi: MangaDexApi
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