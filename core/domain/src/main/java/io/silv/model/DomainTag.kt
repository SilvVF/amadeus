package io.silv.model

import androidx.compose.runtime.Stable
import io.silv.database.entity.list.TagEntity

@Stable
data class DomainTag(
    val group: String,
    val name: String,
    val id: String,
) {
    constructor(entity: TagEntity) : this(
        group = entity.group,
        name = entity.name,
        id = entity.id,
    )
}
