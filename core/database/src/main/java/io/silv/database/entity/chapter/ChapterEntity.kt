package io.silv.database.entity.chapter

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.common.model.ProgressState
import io.silv.common.time.localDateTimeNow
import io.silv.database.entity.AmadeusEntity
import kotlinx.datetime.LocalDateTime

@Entity
data class ChapterEntity(

    @PrimaryKey
    override val id: String,

    @ColumnInfo("manga_id")
    val mangaId: String,
    val progressState: ProgressState = ProgressState.NotStarted,
    val languageCode: String,
    val scanlationGroupId: String?,
    val scanlationGroup: String?,
    val userId: String?,
    val user: String?,
    val volume: Int = -1,
    val lastPageRead: Int,
    val title: String,
    val pages: Int,
    val bookmarked: Boolean,
    val chapterNumber: Long = -1L,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val readableAt: LocalDateTime,
    val uploader: String?,
    val externalUrl: String?,
    val version: Int,
    val savedLocalAt: LocalDateTime = localDateTimeNow()
): AmadeusEntity<Any> {

    val isRecognizedNumber: Boolean
        get() = chapterNumber >= 0f


    val read: Boolean
        get() = progressState == ProgressState.Finished
}
