package io.silv.manga.network.mangadex.models.manga

import android.os.Parcelable
import io.silv.manga.network.mangadex.models.common.Relationship
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Manga(
    val id: String,
    val type: String,
    val attributes: MangaAttributes,
    val relationships: List<Relationship> = emptyList()
): Parcelable
