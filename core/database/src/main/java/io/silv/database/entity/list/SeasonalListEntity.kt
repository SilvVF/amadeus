package io.silv.database.entity.list

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.common.model.Season
import io.silv.database.entity.AmadeusEntity

@Entity
data class SeasonalListEntity(

    @PrimaryKey override val id: String,

    val year: Int,

    val season: Season,

): AmadeusEntity<String>
