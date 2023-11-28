package io.silv.database.entity.list

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.database.entity.AmadeusEntity

@Entity
data class TagEntity(
    @PrimaryKey override val id: String,
    val group: String,
    val name: String,
    val version: Int,
    val lastUpdatedEpochSeconds: Long
): AmadeusEntity<String>
