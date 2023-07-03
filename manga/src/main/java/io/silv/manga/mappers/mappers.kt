import io.silv.manga.domain.models.DomainManga
import io.silv.manga.local.entity.MangaEntity
import io.silv.manga.local.entity.ProgressState
import io.silv.manga.network.mangadex.models.LocalizedString
import io.silv.manga.network.mangadex.models.manga.Manga
import io.silv.manga.sync.Mapper
import kotlinx.datetime.Clock

object MangaEntityToDomainMangaMapper: Mapper<MangaEntity, DomainManga> {

    override fun map(from: MangaEntity): DomainManga {
        return with(from) {
            DomainManga(
                id,
                progressState,
                coverArt,
                titleEnglish,
                alternateTitles,
                originalLanguage,
                availableTranslatedLanguages,
                status,
                contentRating,
                lastVolume,
                lastChapter,
                version,
                bookmarked,
                volumesIds,
                createdAt,
                updatedAt,
                chaptersIds,
                volumeToCoverArt
            )
        }
    }
}

object MangaToMangaEntityMapper: Mapper<Pair<Manga, MangaEntity?>, MangaEntity> {

    override fun map(from: Pair<Manga, MangaEntity?>): MangaEntity {
        val (manga, entity) = from

        val altTitles = buildMap {
            manga.attributes.altTitles.forEach { langToTitle: LocalizedString ->
                put(
                    langToTitle.keys.firstOrNull() ?: return@forEach,
                    langToTitle.values.firstOrNull() ?: return@forEach
                )
            }
        }

        val fileName = manga.relationships.find { it.type == "cover_art" }?.attributes?.get("fileName")

        return entity?.copy(
            titleEnglish = manga.attributes.title.getOrDefault("en", "No english title"),
            coverArt = "https://uploads.mangadex.org/covers/${manga.id}/$fileName",
            alternateTitles =  altTitles,
            availableTranslatedLanguages = manga.attributes.availableTranslatedLanguages.filterNotNull(),
            originalLanguage = manga.attributes.originalLanguage,
            contentRating = manga.attributes.contentRating,
            lastVolume = manga.attributes.lastVolume,
            lastChapter = manga.attributes.lastChapter,
            status = manga.attributes.status,
            version = manga.attributes.version,
            createdAt = manga.attributes.createdAt,
            updatedAt = manga.attributes.updatedAt,
            forList = true,
            savedLocalAtEpochSeconds = Clock.System.now().epochSeconds
        )
            ?: MangaEntity(
                id = manga.id,
                forList = true,
                titleEnglish = manga.attributes.title.getOrDefault("en", "No english title"),
                coverArt =  "https://uploads.mangadex.org/covers/${manga.id}/$fileName",
                alternateTitles =  altTitles,
                availableTranslatedLanguages = manga.attributes.availableTranslatedLanguages.filterNotNull(),
                originalLanguage = manga.attributes.originalLanguage,
                contentRating = manga.attributes.contentRating,
                lastVolume = manga.attributes.lastVolume,
                lastChapter = manga.attributes.lastChapter,
                version = manga.attributes.version,
                createdAt = manga.attributes.createdAt,
                updatedAt = manga.attributes.updatedAt,
                savedLocalAtEpochSeconds = Clock.System.now().epochSeconds,
                progressState = ProgressState.NotStarted,
                status = manga.attributes.status,
                bookmarked = false,
                volumesIds = emptyList(),
                chaptersIds = emptyList(),
                volumeToCoverArt = emptyMap()
            )
    }
}