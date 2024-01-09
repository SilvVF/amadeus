package io.silv.database.entity.chapter

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import io.silv.common.model.ProgressState
import io.silv.common.time.localDateTimeNow
import io.silv.database.entity.AmadeusEntity
import io.silv.database.entity.manga.MangaEntity
import kotlinx.datetime.LocalDateTime

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("manga_id"),
            onDelete = ForeignKey.CASCADE,
        )
    ],
    tableName = "chapters"
)
data class ChapterEntity(
    @PrimaryKey
    override val id: String,
    @ColumnInfo("manga_id")
    val mangaId: String,
    val scanlator: String = "",
    val url: String,
    val title: String,
    val volume: Int = -1,
    val progressState: ProgressState = ProgressState.NotStarted,
    val languageCode: String,
    val scanlationGroupId: String?,
    val userId: String?,
    val user: String?,
    val lastPageRead: Int,
    val pages: Int,
    val bookmarked: Boolean,
    val chapterNumber: Long = -1L,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val readableAt: LocalDateTime,
    val uploader: String?,
    val version: Int,
    val savedLocalAt: LocalDateTime = localDateTimeNow(),
): AmadeusEntity<String> {

    val isRecognizedNumber: Boolean
        get() = chapterNumber >= 0L

    val read: Boolean
        get() = progressState == ProgressState.Finished
}
