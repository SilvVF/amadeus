package io.silv.domain
import androidx.compose.runtime.Stable
import com.skydoves.sandwich.mapSuccess
import io.silv.network.MangaDexApi
import kotlin.math.roundToInt

@Stable
data class MangaStats(
    val follows: Int = 0,
    val rating: Double = 0.0,
    val comments: Int = 0
) {
    val validRating: Boolean
        get() = rating != -1.0

    val validFollows: Boolean
        get() = follows != -1

    val validComments: Boolean
        get() = comments != -1
}

/**
 * Fetches the [MangaStats] for the given manga id from the Manga Dex api.
 * This always fetches from the network.
 */
class GetMangaStatisticsById (
    private val mangaDexApi: MangaDexApi
) {
    suspend operator fun invoke(id: String) = mangaDexApi.getMangaStatistics(id)
        .mapSuccess {
            val stats = statistics.values.toList().firstOrNull()
            MangaStats(
                follows = stats?.follows ?: -1,
                rating = stats?.rating?.average ?: -1.0,
                comments = stats?.comments?.repliesCount?.roundToInt() ?: -1
            )
        }
}