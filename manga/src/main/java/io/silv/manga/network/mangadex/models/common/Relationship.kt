package io.silv.manga.network.mangadex.models.common

import android.os.Parcelable
import io.silv.manga.network.mangadex.models.Related
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class Relationship(
    val id : String,
    val type: String,
    /**
     * Related Manga type, only present if you
     * are on a Manga entity and a Manga relationship
     */
    val related: Related? = null,
    /**
     *
    If Reference Expansion is applied, contains objects attributes
     */
    val attributes: Map<String, String?> = emptyMap()
): Parcelable
