package io.silv.database.entity.manga

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.common.model.ContentRating
import io.silv.common.model.MangaResource
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import io.silv.common.time.epochSeconds
import io.silv.common.time.localDateTimeNow
import io.silv.database.entity.AmadeusEntity
import kotlinx.datetime.LocalDateTime

@Entity(tableName = "manga")
data class MangaEntity(

    @PrimaryKey
    override val id: String,

    @ColumnInfo("cover_art")
    override val coverArt: String,
    override val title: String,
    override val version: Int,
    @ColumnInfo("created_at")
    override val createdAt: LocalDateTime,
    @ColumnInfo("updated_at")
    override val updatedAt: LocalDateTime,

    val favorite: Boolean,
    val description: String,
    @ColumnInfo("alternate_titles")
    val alternateTitles: Map<String, String>,
    @ColumnInfo(name = "original_language")
    val originalLanguage: String,
    @ColumnInfo("available_translated_languages")
    val availableTranslatedLanguages: List<String>,
    val status: Status,
    @ColumnInfo("tag_to_id")
    val tagToId: Map<String, String>,
    @ColumnInfo("content_rating")
    val contentRating: ContentRating,
    @ColumnInfo("last_volume")
    val lastVolume: Int,
    @ColumnInfo("last_chapter")
    val lastChapter: Long,
    @ColumnInfo("publication_demographic")
    val publicationDemographic: PublicationDemographic?,
    val year: Int,
    @ColumnInfo("latest_uploaded_chapter")
    val latestUploadedChapter: String?,
    val authors: List<String>,
    val artists: List<String>,
    @ColumnInfo("progress_state")
    val progressState: ProgressState,
    @ColumnInfo("reading_status")
    val readingStatus: ReadingStatus,

    @ColumnInfo("cover_last_modified")
    val coverLastModified: Long = epochSeconds(),

    @ColumnInfo("saved_at_local")
    val savedAtLocal: LocalDateTime = localDateTimeNow(),

    @ColumnInfo("last_synced_for_updates")
    val lastSyncedForUpdates: LocalDateTime? = null,
) : MangaResource, AmadeusEntity<String>



