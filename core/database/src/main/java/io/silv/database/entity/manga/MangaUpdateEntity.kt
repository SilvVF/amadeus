package io.silv.database.entity.manga

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.common.model.UpdateType
import io.silv.common.time.localDateTimeNow
import kotlinx.datetime.LocalDateTime

@Entity
data class MangaUpdateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "saved_manga_id") val savedMangaId: String,
    @ColumnInfo(name = "update_type") val updateType: UpdateType,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = localDateTimeNow()
)
