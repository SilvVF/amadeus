package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.domain.timeNow
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.LocalDateTime

@Entity
data class FilteredMangaYearlyResource(

    val topTags: List<String>,

    val topTagPlacement: Map<String, Int>,

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

    override val savedAtLocal: LocalDateTime = timeNow(),
    override val year: Int,
    override val latestUploadedChapter: String?,
    override val authors: List<String>,
    override val artists: List<String>,
    ): MangaResource, AmadeusEntity<Any?>