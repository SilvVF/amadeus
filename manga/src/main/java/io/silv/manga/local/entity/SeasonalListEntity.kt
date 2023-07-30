package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SeasonalListEntity(
    @PrimaryKey override val id: String,
    val year: Int,
    val season: Season,
): AmadeusEntity<String>

enum class Season{
    Winter, Spring, Summer, Fall
}