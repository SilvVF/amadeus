@file:Suppress("unused")

package io.silv.manga.network.mangadex.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

enum class Result {
    ok, error
}

enum class State {
    draft, submitted, published, rejected
}

enum class Group {
    content, format, genre, theme
}

@Parcelize
enum class ContentRating : Parcelable, Serializable {
    safe, suggestive, erotica, pornographic
}

enum class Status {
    completed, ongoing, cancelled, hiatus
}

enum class Related {
    monochrome, main_story, adapted_from, based_on, prequel, side_story, doujinshi, same_franchise, shared_universe, sequel, spin_off, alternate_story, alternate_version, preserialization, colored, serialization
}

enum class PublicationDemographic {
    shounen, shoujo, josei, seinen
}

enum class CoverIncludesFilter {
    manga, user
}
