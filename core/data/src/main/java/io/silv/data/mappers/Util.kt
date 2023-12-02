package io.silv.data.mappers

import io.silv.common.model.TimePeriod
import io.silv.common.time.timeStringMinus
import io.silv.network.model.LocalizedString
import io.silv.network.model.manga.Manga
import kotlin.time.Duration.Companion.days

fun coverArtUrl(
    fileName: String?,
    mangaId: String,
) =  "https://uploads.mangadex.org/covers/${mangaId}/$fileName"

fun TimePeriod.timeString(): String? {
    return timeStringMinus(
            when (this) {
                TimePeriod.SixMonths -> (6 * 30).days
                TimePeriod.ThreeMonths ->  (3 * 30).days
                TimePeriod.LastMonth -> 30.days
                TimePeriod.OneWeek -> 7.days
                TimePeriod.AllTime -> return null
            }
    )
}

fun coverArtUrl(
    manga: Manga,
): String {
    val fileName = manga.relationships
        .find { it.type == "cover_art" }
        ?.attributes
        ?.fileName
        ?: ""

    return "https://uploads.mangadex.org/covers/${manga.id}/$fileName"
}

val Manga.titleEnglish: String
    get() = attributes.title.getOrElse("en") {
        attributes.altTitles.firstNotNullOfOrNull { it["en"] ?: it["ja-ro"] ?: it["ja"] } ?: ""
    }

val Manga.tagToId: Map<String, String>
    get() = attributes.tags
        .filter { it.type == "tag" }
        .mapNotNull {
            (it.attributes.name["en"] ?: it.attributes.name["ja-ro"] ?: return@mapNotNull null) to it.id
        }
        .toMap()

val Manga.descriptionEnglish: String
    get() = attributes.description.getOrDefault("en", "No english description")

val Manga.artists: List<String>
    get() = this.relationships.filter { it.type == "artist" }
        .map {
            it.attributes?.name ?: ""
        }


val Manga.authors: List<String>
    get() = this.relationships.filter { it.type == "author" }
        .map {
            it.attributes?.name ?: ""
        }


val Manga.alternateTitles: Map<String, String>
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
