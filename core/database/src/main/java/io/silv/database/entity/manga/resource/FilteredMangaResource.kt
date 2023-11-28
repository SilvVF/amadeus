package io.silv.database.entity.manga.resource

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.common.model.ContentRating
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.Status
import io.silv.common.time.localDateTimeNow
import io.silv.database.entity.AmadeusEntity
import io.silv.database.entity.manga.MangaResource
import kotlinx.datetime.LocalDateTime


@Entity
data class FilteredMangaResource(

    val offset: Int,

    @PrimaryKey override val id: String,
    override val coverArt: String,
    override val description: String,
    override val titleEnglish: String,
    override val alternateTitles: Map<String, String>,
    override val originalLanguage: String,
    override val availableTranslatedLanguages: List<String>,
    override val status: Status,
    override val tagToId: Map<String, String>,
    override val contentRating: ContentRating,
    override val lastVolume: Int,
    override val lastChapter: Long,
    override val version: Int,
    override val createdAt: LocalDateTime,
    override val publicationDemographic: PublicationDemographic?,
    override val updatedAt: LocalDateTime,
    override val volumeToCoverArt: Map<String, String> = emptyMap(),
    override val savedAtLocal: LocalDateTime = localDateTimeNow(),
    override val year: Int = 1,
    override val latestUploadedChapter: String?,
    override val authors: List<String>,
    override val artists: List<String>,
): MangaResource, AmadeusEntity<String>

