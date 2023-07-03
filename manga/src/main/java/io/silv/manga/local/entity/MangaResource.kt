package io.silv.manga.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.silv.manga.network.mangadex.models.ContentRating
import io.silv.manga.network.mangadex.models.Status
import io.silv.manga.sync.AmadeusEntity
import kotlinx.datetime.Clock

@Entity
data class MangaResource(

    @PrimaryKey override val id: String,

    val coverArt: String,

    val description: String,

    val titleEnglish: String,

    val alternateTitles: Map<String, String>,

    val originalLanguage: String,

    val availableTranslatedLanguages: List<String>,

    val status: Status,

    val contentRating: ContentRating,

    val lastVolume: String? = null,

    val lastChapter: String? = null,

    val version: Int,

    val createdAt: String,

    val updatedAt: String,

    val savedLocalAtEpochSeconds: Long = Clock.System.now().epochSeconds
): AmadeusEntity
