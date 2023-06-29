package io.silv.amadeus.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MangaEntity(
    @PrimaryKey val mid: String,
    @ColumnInfo val volumes: List<String>,
)
