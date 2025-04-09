package io.silv.data.mappers

import io.silv.common.log.logcat
import io.silv.common.model.TimePeriod
import io.silv.common.time.timeStringMinus
import io.silv.network.model.LocalizedString
import io.silv.network.model.manga.MangaDto
import kotlin.time.Duration.Companion.days

fun TimePeriod.timeString(): String? {
    return timeStringMinus(
            when (this) {
                TimePeriod.AllTime -> return null
                TimePeriod.OneYear -> 360.days
                TimePeriod.SixMonths -> (6 * 30).days
                TimePeriod.ThreeMonths ->  (3 * 30).days
                TimePeriod.LastMonth -> 30.days
                TimePeriod.OneWeek -> 7.days
            }
    )
}

fun coverArtUrl(
    mangaDto: MangaDto,
): String {
    val fileName = mangaDto.relationships
        .find { it.type == "cover_art" }
        ?.attributes
        ?.fileName
        ?: ""

    logcat("MangaCoverArt") { "$fileName ${mangaDto.relationships}" }
    logcat("MangaCoverArt") { "coverArtUrl ${"https://uploads.mangadex.org/covers/${mangaDto.id}/$fileName"}" }

    return "https://uploads.mangadex.org/covers/${mangaDto.id}/$fileName"
}

val MangaDto.titleEnglish: String
    get() = attributes.title.getOrElse("en") {
        attributes.altTitles.firstNotNullOfOrNull { it["en"] ?: it["ja-ro"] ?: it["ja"] } ?: ""
    }

val MangaDto.tagToId: Map<String, String>
    get() = attributes.tags
        .filter { it.type == "tag" }
        .mapNotNull {
            (it.attributes.name["en"] ?: it.attributes.name["ja-ro"] ?: return@mapNotNull null) to it.id
        }
        .toMap()

val MangaDto.descriptionEnglish: String
    get() = attributes.description.getOrDefault("en", "No english description")

val MangaDto.artists: List<String>
    get() = this.relationships.filter { it.type == "artist" }
        .map {
            it.attributes?.name ?: ""
        }


val MangaDto.authors: List<String>
    get() = this.relationships.filter { it.type == "author" }
        .map {
            it.attributes?.name ?: ""
        }


val MangaDto.alternateTitles: Map<String, String>
    get() {
        return buildMap {
            attributes.altTitles.forEach { langToTitle: LocalizedString ->
                put(
                    langToTitle.keys.firstOrNull() ?: return@forEach,
                    langToTitle.values.firstOrNull() ?: return@forEach
                )
            }
        }
    }
