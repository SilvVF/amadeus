package io.silv.amadeus.domain.mappers

import io.silv.amadeus.domain.models.DomainChapter
import io.silv.amadeus.domain.models.DomainManga
import io.silv.amadeus.network.mangadex.models.Group
import io.silv.amadeus.network.mangadex.models.chapter.Chapter

fun toDomainChapter(
    chapter: Chapter,
): DomainChapter {
    val it = chapter
    return DomainChapter(
        id = chapter.id.also { println(it) },
        title = it.attributes.title,
        volume = it.attributes.volume,
        downloaded = false,
        chapter = it.attributes.chapter,
        pages = it.attributes.pages,
        translatedLanguage = it.attributes.translatedLanguage,
        updatedAt = it.attributes.updatedAt,
        uploader = it.attributes.uploader ?: "",
        externalUrl = it.attributes.externalUrl,
        version = it.attributes.version,
        createdAt = it.attributes.createdAt,
        readableAt = it.attributes.readableAt,
        mangaId = it.attributes.relationships.find {
            it.type == "manga"
        }?.id ?: ""
    )
}

fun toDomainManga(networkManga: io.silv.amadeus.network.mangadex.models.manga.Manga): DomainManga {

    val fileName = networkManga.relationships.find {
        it.type == "cover_art"
    }?.attributes?.get("fileName")

    val genres = networkManga.attributes.tags.filter {
        it.attributes.group == Group.genre
    }.map {
        it.attributes.name["en"] ?: ""
    }

    val titles = buildMap {
        networkManga.attributes.altTitles.forEach {
            for ((k, v) in it) {
                put(k, v)
            }
        }
    }

    return DomainManga(
        id = networkManga.id,
        description = networkManga.attributes.description.getOrDefault("en", ""),
        title = networkManga.attributes.title.getOrDefault("en", ""),
        imageUrl = "https://uploads.mangadex.org/covers/${networkManga.id}/$fileName",
        genres = genres,
        altTitle = networkManga.attributes.altTitles.find { it.containsKey("en") }?.getOrDefault("en", "") ?: "",
        availableTranslatedLanguages = networkManga.attributes.availableTranslatedLanguages.filterNotNull(),
        allDescriptions = networkManga.attributes.description,
        allTitles = titles,
        lastChapter = networkManga.attributes.lastChapter?.toIntOrNull() ?: 0,
        lastVolume = networkManga.attributes.lastVolume?.toIntOrNull() ?: 0,
        status = networkManga.attributes.status,
        year = networkManga.attributes.year ?: 0,
        contentRating = networkManga.attributes.contentRating,
    )
}