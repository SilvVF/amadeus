package io.silv.amadeus.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import io.silv.amadeus.network.mangadex.MangaDexTestApi
import io.silv.amadeus.network.mangadex.models.chapter.Chapter
import io.silv.amadeus.network.mangadex.models.manga.Manga
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import io.silv.amadeus.ui.shared.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeSM(
    private val api: MangaDexTestApi
): AmadeusScreenModel<HomeEvent, HomeState>(HomeState()) {

    private val chaptersList = MutableStateFlow(emptyList<Chapter>())
    private val mangaList = MutableStateFlow(emptyList<Manga>())

    init {
        coroutineScope.launch {
            combine(mangaList, chaptersList) { mangas, chapters ->
                state.value.copy(
                    mangaList = mangas,
                    chapterList = chapters
                )
            }.collect {
                mutableState.emit(it)
            }
        }
    }

    fun loadData() = coroutineScope.launch {
        chaptersList.emit(
            api.getChapterList().data
        )
        mangaList.emit(
            api.getMangaList().data
        )
    }
}

sealed interface HomeEvent

data class HomeState(
    val mangaList: List<Manga> = emptyList(),
    val chapterList: List<Chapter> = emptyList()
)

class HomeScreen: Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<HomeSM>()

        val state by sm.state.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            sm.loadData()
        }

        Row {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                items(
                    items = state.mangaList
                ) {
                    Text(
                        text = it.attributes.title.toString()
                    )
                    Text(text = it.attributes.altTitles.toString())
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                items(
                    items = state.chapterList
                ) {
                    Text(
                        text = it.type
                    )
                    Text(text = it.id)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

    }
}