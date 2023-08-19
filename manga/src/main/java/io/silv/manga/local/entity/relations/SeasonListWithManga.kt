package io.silv.manga.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.manga.local.entity.SeasonalListEntity
import io.silv.manga.local.entity.manga_resource.SeasonalMangaResource

data class SeasonListWithManga(

    @Embedded val list: SeasonalListEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "season_id",
    )
    val manga: List<SeasonalMangaResource>
)

