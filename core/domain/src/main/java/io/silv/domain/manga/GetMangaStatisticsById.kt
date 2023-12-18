package io.silv.domain.manga
import com.skydoves.sandwich.mapSuccess
import io.silv.model.MangaStats
import io.silv.network.MangaDexApi
import kotlin.math.roundToInt

/**
 * Fetches the [MangaStats] for the given manga id from the Manga Dex api.
 * This always fetches from the network.
 */
class GetMangaStatisticsById(
    private val mangaDexApi: MangaDexApi,
) {
    suspend fun await(id: String) =
        mangaDexApi.getMangaStatistics(id)
            .mapSuccess {
                val stats = statistics.values.toList().firstOrNull()
                MangaStats(
                    follows = stats?.follows ?: -1,
                    rating = stats?.rating?.average ?: -1.0,
                    comments = stats?.comments?.repliesCount?.roundToInt() ?: -1,
                )
            }
}
