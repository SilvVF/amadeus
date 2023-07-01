@file:OptIn(ExperimentalAnimationApi::class)

package io.silv.amadeus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.amadeus.ui.screens.HomeScreen
import io.silv.amadeus.ui.theme.AmadeusTheme

class MainActivity : ComponentActivity() {


//    private val testApi by inject<MangaDexTestApi>()
//
//    private fun toDomainManga(networkManga: io.silv.amadeus.network.mangadex.models.manga.Manga): DomainManga {
//
//        val fileName = networkManga.relationships.find {
//            it.type == "cover_art"
//        }?.attributes?.get("fileName")
//
//        val genres = networkManga.attributes.tags.filter {
//            it.attributes.group == Group.genre
//        }.map {
//            it.attributes.name["en"] ?: ""
//        }
//
//        val titles = buildMap {
//            networkManga.attributes.altTitles.forEach {
//                for ((k, v) in it) {
//                    put(k, v)
//                }
//            }
//        }
//
//        return DomainManga(
//            id = networkManga.id,
//            description = networkManga.attributes.description.getOrDefault("en", ""),
//            title = networkManga.attributes.title.getOrDefault("en", ""),
//            imageUrl = "https://uploads.mangadex.org/covers/${networkManga.id}/$fileName",
//            genres = genres,
//            altTitle = networkManga.attributes.altTitles.find { it.containsKey("en") }?.getOrDefault("en", "") ?: "",
//            availableTranslatedLanguages = networkManga.attributes.availableTranslatedLanguages.filterNotNull(),
//            allDescriptions = networkManga.attributes.description,
//            allTitles = titles,
//            lastChapter = networkManga.attributes.lastChapter?.toIntOrNull() ?: 0,
//            lastVolume = networkManga.attributes.lastVolume?.toIntOrNull() ?: 0,
//            status = networkManga.attributes.status,
//            year = networkManga.attributes.year ?: 0,
//            contentRating = networkManga.attributes.contentRating,
//        )
//    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
//
//        val manga = runBlocking {
//            testApi.getMangaById().data
//        }

        setContent {
            AmadeusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigator(
                        screen = HomeScreen()
                    ) {
                        FadeTransition(navigator = it)
                    }
                }
            }
        }
    }
}

