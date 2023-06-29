package io.silv.amadeus.local.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.amadeus.local.entity.ChapterEntity
import io.silv.amadeus.local.entity.VolumeEntity

data class VolumeWithChapters(

    @Embedded val volume: VolumeEntity,

    @Relation(
        entity = ChapterEntity::class,
        parentColumn = "vid",
        entityColumn = "volume_id"
    )
    val chapters: List<ChapterEntity>
)
