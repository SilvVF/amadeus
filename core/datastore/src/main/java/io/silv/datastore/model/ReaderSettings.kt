package io.silv.datastore.model

import io.silv.common.model.ReaderDirection
import io.silv.common.model.ReaderOrientation
import kotlinx.serialization.Serializable

@Serializable
data class ReaderSettings(
    val orientation: ReaderOrientation = ReaderOrientation.Horizontal,
    val direction: ReaderDirection = ReaderDirection.Ltr,
)

@Serializable
data class Filters(
    val downloaded: Boolean = false,
    val unread: Boolean = false,
    val bookmarked: Boolean = false,
    val bySourceAsc: Boolean? = null,
    val byChapterAsc: Boolean? = true,
    val byUploadDateAsc: Boolean? = null,
)
