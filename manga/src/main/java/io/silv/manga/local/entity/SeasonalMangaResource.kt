package io.silv.manga.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.PublicationDemographic
import io.silv.manga.network.mangadex.models.Status
import kotlinx.datetime.Clock

@Entity
data class SeasonalMangaResource(

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

    override val lastVolume: String? = null,

    override val lastChapter: String? = null,

    override val version: Int,

    override val createdAt: String,

    override val updatedAt: String,

    override val publicationDemographic: PublicationDemographic?,

    override val volumeToCoverArt: Map<String, String> = emptyMap(),

    override val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds,

    @ColumnInfo("season_id")val seasonId: String,

    override val year: Int?,
    override val latestUploadedChapter: String?,
    override val authors: List<String>,
    override val artists: List<String>
): MangaResource, AmadeusEntity<Any?>