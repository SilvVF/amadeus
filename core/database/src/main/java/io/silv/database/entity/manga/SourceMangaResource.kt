package io.silv.database.entity.manga

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.common.model.ContentRating
import io.silv.common.model.MangaResource
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import io.silv.common.time.localDateTimeNow
import io.silv.database.entity.AmadeusEntity
import kotlinx.datetime.LocalDateTime

@Entity
data class SourceMangaResource(
    @PrimaryKey
    override val id: String,
    override val coverArt: String,
     val description: String,
     override val title: String,
     val alternateTitles: Map<String, String>,
     val originalLanguage: String,
     val availableTranslatedLanguages: List<String>,
     val status: Status,
     val tagToId: Map<String, String>,
     val contentRating: ContentRating,
     val lastVolume: Int,
     val lastChapter: Long,
    override val version: Int,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
     val publicationDemographic: PublicationDemographic?,
     val volumeToCoverArt: Map<String, String> = emptyMap(),
     val savedAtLocal: LocalDateTime = localDateTimeNow(),
     val year: Int,
     val latestUploadedChapter: String?,
     val authors: List<String>,
     val artists: List<String>,
     val progressState: ProgressState,
     val readingStatus: ReadingStatus,
): MangaResource, AmadeusEntity<String>