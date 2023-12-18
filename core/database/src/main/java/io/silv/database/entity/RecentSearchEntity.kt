package io.silv.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,
    val queriedDate: Instant,
)
