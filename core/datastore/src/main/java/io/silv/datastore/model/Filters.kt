package io.silv.datastore.model

import kotlinx.serialization.Serializable


@Serializable
data class Filters(
    val downloaded: Boolean = false,
    val unread: Boolean = false,
    val bookmarked: Boolean = false,
    val bySourceAsc: Boolean? = null,
    val byChapterAsc: Boolean? = true,
    val byUploadDateAsc: Boolean? = null,
)
