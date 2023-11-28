package io.silv.network.model.common

import android.os.Parcelable
import io.silv.common.model.Group
import io.silv.network.model.LocalizedString
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