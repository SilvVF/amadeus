package io.silv.amadeus.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import io.silv.amadeus.network.MangaApi
import io.silv.amadeus.network.models.manga.Manga
import io.silv.amadeus.network.requests.MangaRequest
import io.silv.amadeus.network.requests.createQueryParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeSM(
    private val api: MangaApi
): ScreenModel {

    private val mutableMangaList = MutableStateFlow<List<Manga>>(emptyList())
    val mangaList = mutableMangaList.asStateFlow()

    init {
        try {
            MangaRequest().createQueryParams().also {
                Log.d("MANGA", it.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMangaList() = coroutineScope.launch {
        mutableMangaList.emit(
            api.getTestMangaList().data
        )
    }
}

class HomeScreen: Screen {

    @Composable
    override fun Content() {

        val sm = getScreenModel<HomeSM>()

        val mangas by sm.mangaList.collectAsState()

        LaunchedEffect(Unit) {
            sm.getMangaList()
        }

        LazyColumn(
            Modifier.fillMaxSize(),
        ) {
            items(
                items = mangas
            ) {
                Text(
                    text = it.attributes.title.toString()
                )
                Text(text = it.attributes.altTitles.toString())
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}