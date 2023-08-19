package io.silv.amadeus.types

import io.silv.manga.local.entity.TagEntity

data class DomainTag(
    val group: String,
    val name: String,
    val id: String
) {

    constructor(entity: TagEntity): this(
        group = entity.group,
        name = entity.name,
        id = entity.id
    )
}