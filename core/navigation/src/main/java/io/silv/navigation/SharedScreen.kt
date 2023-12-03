package io.silv.navigation

import cafe.adriel.voyager.core.registry.ScreenProvider

sealed class SharedScreen : ScreenProvider {

    data object Explore : SharedScreen()

    data object Library: SharedScreen()

    data class Reader(
        val mangaId: String,
        val initialChapterId: String,
    ): SharedScreen()

    data class MangaView(
        val mangaId: String
    ): SharedScreen()

    data class MangaFilter(
        val tag: String,
        val tagId: String,
    ): SharedScreen()

    data object MangaSearch: SharedScreen()
}

