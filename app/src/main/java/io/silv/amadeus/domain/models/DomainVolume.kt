package io.silv.amadeus.domain.models

import android.os.Parcelable
import io.silv.amadeus.local.entity.TrackingState
import kotlinx.parcelize.Parcelize


@Parcelize
data class DomainVolume(
    val mangaId: String,
    val trackingState: TrackingState = TrackingState.NotTracked,
    val number: Int,
    val count: Int,
    val chapters: List<DomainChapter>,
) : Parcelable

