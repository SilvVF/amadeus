package eu.kanade.tachiyomi

import coil.key.Keyer
import coil.request.Options
import io.silv.common.model.MangaCover
import io.silv.model.SavableManga
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

fun SavableManga.asMangaCover(): MangaCover {
    return MangaCover(
        mangaId = id,
        url = coverArt,
        isMangaFavorite = inLibrary,
        lastModified = savedLocalAtEpochSeconds.toInstant(TimeZone.currentSystemDefault()).epochSeconds,
    )
}

fun SavableManga.hasCustomCover(): Boolean {
    return false
}

class MangaKeyer : Keyer<SavableManga> {
    override fun key(
        data: SavableManga,
        options: Options,
    ): String {
        return if (data.hasCustomCover()) {
            "${data.id};${data.savedLocalAtEpochSeconds}"
        } else {
            "${data.coverArt};${data.savedLocalAtEpochSeconds}"
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
        return if (coverCache.getCustomCoverFile(data.mangaId).exists()) {
            "${data.mangaId};${data.lastModified}"
        } else {
            "${data.url};${data.lastModified}"
        }
    }
}
