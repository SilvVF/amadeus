package io.silv.database.entity.chapter

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
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
    @ColumnInfo("manga_id", index = true)
    val mangaId: String,
    val scanlator: String = "",
    val url: String,
    val title: String,
    val volume: Int = -1,
    val languageCode: String,
    @ColumnInfo("scanlation_group_id")
    val scanlationGroupId: String?,
    @ColumnInfo("user_id")
    val userId: String?,
    val user: String?,
    @ColumnInfo("last_page_read")
    val lastPageRead: Int?,
    val pages: Int,
    val bookmarked: Boolean,
    @ColumnInfo("chapter_number")
    val chapterNumber: Double = -1.0,
    @ColumnInfo("created_at")
    val createdAt: LocalDateTime,
    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime,
    @ColumnInfo("readable_at")
    val readableAt: LocalDateTime,
    val uploader: String?,
    val version: Int,
    @ColumnInfo("saved_local_at")
    val savedLocalAt: LocalDateTime = localDateTimeNow(),
): AmadeusEntity<String> {

    val isRecognizedNumber: Boolean
        get() = chapterNumber >= 0L

    val read: Boolean
        get() = lastPageRead == pages
}
