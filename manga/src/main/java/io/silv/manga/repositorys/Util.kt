package io.silv.manga.repositorys

import android.util.Log
import io.silv.manga.network.mangadex.models.LocalizedString
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.repositorys.manga.FilteredMangaRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.toKotlinDuration



/**
 * Attempts [block], returning a successful [Result] if it succeeds, otherwise a [Result.Failure]
 * taking care not to break structured concurrency
 */
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (exception: Exception) {
    Log.i(
        "suspendRunCatching",
        "Failed to evaluate a suspendRunCatchingBlock. Returning failure Result",
        exception,
    )
    Result.failure(exception)
}


fun String.parseMangaDexTimeToDateTime(): LocalDateTime {
    // "2021-10-10T23:19:03+00:00",
    val text = this.replaceAfter('+', "").dropLast(1)
    return LocalDateTime.parse(text)
}

operator fun LocalDateTime.minus(localDateTime: LocalDateTime): kotlin.time.Duration {
    return this.toInstant(timeZone())
        .minus(
            localDateTime.toInstant(timeZone())
        )
}


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
        ?.fileName

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

fun epochSeconds() = Clock.System.now().epochSeconds

fun timeZone() = TimeZone.currentSystemDefault()

fun timeNow() = Clock.System.now().toLocalDateTime(timeZone())

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

