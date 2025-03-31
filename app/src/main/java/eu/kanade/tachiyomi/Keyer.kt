package eu.kanade.tachiyomi

import coil.key.Keyer
import coil.request.Options
import io.silv.common.model.MangaCover
import io.silv.data.download.CoverCache
import io.silv.domain.manga.model.Manga
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

fun Manga.asMangaCover(): MangaCover {
    return MangaCover(
        mangaId = id,
        url = coverArt,
        isMangaFavorite = inLibrary,
        lastModified = coverLastModified,
    )
}


class MangaKeyer : Keyer<Manga> {
    override fun key(
        data: Manga,
        options: Options,
    ): String {
        return "${data.coverArt};${data.coverLastModified}"
    }
}

class MangaCoverKeyer(
    private val coverCache: CoverCache,
) : Keyer<MangaCover> {
    override fun key(
        data: MangaCover,
        options: Options,
    ): String {
        return if (coverCache.getCustomCoverFile(data.mangaId).exists()) {
            "${data.mangaId};${data.lastModified}"
        } else {
            "${data.url};${data.lastModified}"
        }
    }
}
