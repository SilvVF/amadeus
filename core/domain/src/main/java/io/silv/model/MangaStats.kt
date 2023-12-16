package io.silv.model

import androidx.compose.runtime.Stable

@Stable
data class MangaStats(
    val follows: Int = 0,
    val rating: Double = 0.0,
    val comments: Int = 0
) {
    val validRating: Boolean
        get() = rating != -1.0

    val validFollows: Boolean
        get() = follows != -1

    val validComments: Boolean
        get() = comments != -1
}
