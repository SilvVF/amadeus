package eu.kanade.tachiyomi

import coil3.key.Keyer
import coil3.request.Options
import io.silv.common.model.MangaCover
import io.silv.data.download.CoverCache
import io.silv.domain.manga.model.Manga

fun Manga.asMangaCover(): MangaCover {
    return MangaCover(
        mangaId = id,
        url = coverArt,
        isMangaFavorite = inLibrary,
        lastModified = coverLastModified,
    )
}


class MangaKeyer(
    private val coverCache: CoverCache
) : Keyer<Manga> {
    override fun key(data: Manga, options: Options): String {
        return if (data.inLibrary && coverCache.getCustomCoverFile(data.id).exists()) {
            "${data.id};${data.coverLastModified}"
        } else {
            "${data.coverArt};${data.coverLastModified}"
        }
    }
}

class MangaCoverKeyer(
    private val coverCache: CoverCache,
) : Keyer<MangaCover> {
    override fun key(
        data: MangaCover,
        options: Options,
    ): String {
        return if (data.isMangaFavorite && coverCache.getCustomCoverFile(data.mangaId).exists()) {
            "${data.mangaId};${data.lastModified}"
        } else {
            "${data.url};${data.lastModified}"
        }
    }
}
