package io.silv.manga.network.mangadex.models.common

import android.os.Parcelable
import io.silv.manga.network.mangadex.models.Group
import io.silv.manga.network.mangadex.models.LocalizedString
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TagAttributes(
    val name: LocalizedString,
    val description: LocalizedString,
    val group: Group,
    val version: Int,
    val relationships: List<Relationship> = emptyList()
): Parcelable