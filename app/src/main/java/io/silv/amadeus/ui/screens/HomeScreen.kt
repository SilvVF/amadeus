package io.silv.amadeus.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.amadeus.filterBothNotNull
import io.silv.amadeus.network.mangadex.MangaDexApi
import io.silv.amadeus.network.mangadex.models.cover.Cover
import io.silv.amadeus.network.mangadex.requests.MangaRequest
import io.silv.amadeus.pmap
import io.silv.amadeus.ui.shared.AmadeusScreenModel
import kotlinx.coroutines.launch

class HomeSM(
    private val api: MangaDexApi
): AmadeusScreenModel<HomeEvent, HomeState>(HomeState()) {

    init {
        coroutineScope.launch {

            mutableState.emit(
                HomeState(
                    data = api.getMangaList(
                        MangaRequest(includes = listOf("cover_art"))
                    ).data.map {
                        val fileName = it.relationships.find {
                            it.type == "cover_art"
                        }?.attributes?.get("fileName") ?: return@map null
                        return@map it.id to fileName
                    }
                        .filterNotNull()

                )
            )
        }
    }

}

sealed interface HomeEvent

data class HomeState(
    val data: List<Pair<String, String>> = emptyList()
)

class HomeScreen: Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<HomeSM>()

        val state by sm.state.collectAsState()

        val ctx = LocalContext.current

        LazyColumn(
            Modifier
                .fillMaxSize(),
        ) {
                items(
                    items = state.data,
                    key = { item -> item.second }
                ) {(mangaId, cover) ->
                    Text(
                        text = cover.toString()
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data("https://uploads.mangadex.org/covers/$mangaId/$cover")
                            .crossfade(true)
                            .build(),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
        }

    }
}

