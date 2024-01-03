package io.silv.database.entity.list

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.database.entity.manga.MangaToListRelation

data class SeasonListKeys(
    @Embedded val list: SeasonalListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "season_id",
    )
    val keys: List<MangaToListRelation>,
)
