package io.silv.amadeus.domain.models

import android.os.Parcelable
import io.silv.amadeus.local.entity.ProgressState
import io.silv.amadeus.local.entity.TrackingState
import io.silv.amadeus.network.mangadex.models.ContentRating
import io.silv.amadeus.network.mangadex.models.Status
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class DomainManga(
    val id: String,
    val progress: ProgressState = ProgressState.NotStarted,
    val trackingState: TrackingState = TrackingState.NotTracked,
    val title: String,
    val altTitle: String,
    val availableTranslatedLanguages: List<String>,
    val allTitles: Map<String, String>,
    val allDescriptions: Map<String, String>,
    val description: String,
    val imageUrl: String,
    val lastVolume: Int,
    val lastChapter: Int,
    val status: Status,
    val year: Int,
    val contentRating: ContentRating,
    val genres: List<String>
): Parcelable, Serializable