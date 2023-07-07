package io.silv.manga.domain

import io.silv.manga.network.mangadex.models.LocalizedString
import io.silv.manga.network.mangadex.models.manga.Manga


fun coverArtUrl(
    fileName: String?,
    mangaId: String,
) =  "https://uploads.mangadex.org/covers/${mangaId}/$fileName"


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
    get() = attributes.title.getOrDefault("en", "No english title")

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

