package io.silv.database.entity.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.silv.database.entity.chapter.ChapterEntity
import kotlinx.datetime.LocalDateTime

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("chapter_id"),
            onDelete = ForeignKey.CASCADE,
        )
    ],
    tableName = "history"
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "chapter_id", index = true)
    val chapterId: String,
    @ColumnInfo("last_read")
    val lastRead: LocalDateTime,
    @ColumnInfo("time_read")
    val timeRead: Long,
)


