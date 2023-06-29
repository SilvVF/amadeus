package io.silv.amadeus.local.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.amadeus.local.entity.MangaEntity
import io.silv.amadeus.local.entity.VolumeEntity

data class MangaWithVolumes(

    @Embedded val manga: MangaEntity,

    @Relation(
        parentColumn = "mid",
        entityColumn = "manga_id"
    )
    val volumes: List<VolumeEntity>
)

