package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TagEntity(
    @PrimaryKey override val id: String,
    val group: String,
    val name: String,
    val version: Int,
    val lastUpdatedEpochSeconds: Long
): AmadeusEntity<String>
