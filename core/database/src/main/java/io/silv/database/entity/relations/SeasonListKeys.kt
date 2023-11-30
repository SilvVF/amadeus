package io.silv.database.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import io.silv.database.entity.list.SeasonalListEntity
import io.silv.database.entity.manga.remotekeys.SeasonalRemoteKey

data class SeasonListKeys(

    @Embedded val list: SeasonalListEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "season_id",
    )
    val keys: List<SeasonalRemoteKey>
)



