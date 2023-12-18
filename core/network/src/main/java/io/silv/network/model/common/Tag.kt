package io.silv.network.model.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Tag(
    val id: String,
    val type: String,
    val attributes: TagAttributes,
    val relationships: List<Relationship> = emptyList(),
) : Parcelable
