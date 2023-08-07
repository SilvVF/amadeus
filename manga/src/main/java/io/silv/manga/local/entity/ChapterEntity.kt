package io.silv.manga.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

@Entity
data class ChapterEntity(

    @PrimaryKey override val id: String,

    @ColumnInfo("manga_id")
    val mangaId: String,

    val progressState: ProgressState = ProgressState.NotStarted,

    val languageCode: String,

    val scanlationGroupId: String?,

    val scanlationGroup: String?,

    val userId: String?,

    val user: String?,

    val volume: String?,

    val title: String,

    val pages: Int,

    val chapterNumber: Double = 0.0,

    val chapterImages: List<String> = emptyList(),

    val createdAt: String,

    val updatedAt: String,

    val readableAt: String,

    val uploader: String?,

    val externalUrl: String?,

    val version: Int,

    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): AmadeusEntity<Any?>
