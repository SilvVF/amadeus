package io.silv.manga.domain

import io.silv.manga.domain.repositorys.FilteredMangaRepository
import io.silv.manga.domain.repositorys.base.ProtectedResources
import io.silv.manga.network.mangadex.models.LocalizedString
import io.silv.manga.network.mangadex.models.manga.Manga
import kotlinx.datetime.Clock
import java.time.Duration
import kotlin.time.toKotlinDuration


internal fun checkProtected(id: String) = ProtectedResources.ids.contains(id)

fun coverArtUrl(
    fileName: String?,
    mangaId: String,
) =  "https://uploads.mangadex.org/covers/${mangaId}/$fileName"

fun FilteredMangaRepository.TimePeriod.timeString(): String? {
    return timeStringMinus(
        Duration.ofDays(
            when (this) {
                FilteredMangaRepository.TimePeriod.SixMonths -> 6 * 30
                FilteredMangaRepository.TimePeriod.ThreeMonths ->  3 * 30
                FilteredMangaRepository.TimePeriod.LastMonth -> 30
                FilteredMangaRepository.TimePeriod.OneWeek -> 7
                else -> return null
            }
        )
    )
}

fun coverArtUrl(
    manga: Manga,
): String {
    val fileName = manga.relationships
        .find { it.type == "cover_art" }
        ?.attributes
        ?.get("fileName")

    return "https://uploads.mangadex.org/covers/${manga.id}/$fileName"
}

val Manga.titleEnglish: String
    get() = attributes.title.getOrElse("en") {
        attributes.altTitles.firstNotNullOfOrNull { it["ja-ro"] } ?: "No english title"
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

fun timeStringMinus(duration: kotlin.time.Duration): String {
    return Clock.System.now()
        .minus(
            duration
        )
        .toString()
        .replace(":", "%3A")
        .takeWhile {
            it != '.'
        }
}

fun timeStringMinus(duration: Duration): String {
    return Clock.System.now()
        .minus(
            duration.toKotlinDuration()
        )
        .toString()
        .replace(":", "%3A")
        .takeWhile {
            it != '.'
        }
}

