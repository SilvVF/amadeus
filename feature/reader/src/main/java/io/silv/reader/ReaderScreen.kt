package io.silv.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import eu.kanade.tachiyomi.reader.model.ReaderChapter
import io.silv.ui.CenterBox
import org.koin.core.parameter.parametersOf

class ReaderScreen(
    val mangaId: String,
    val chapterId: String
): Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<ReaderScreenModel> { parametersOf(mangaId, chapterId) }

        when (val state = screenModel.state.collectAsStateWithLifecycle().value) {
            is ReaderState.Error -> CenterBox {
                Text(state.reason)
            }
            ReaderState.Loading -> CenterBox(Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
            is ReaderState.Success -> {
                Chapter(chapter = state.chapter)
            }
        }
    }
}

@Composable
fun Chapter(
    chapter: ReaderChapter
) {

    when (val chapterState = chapter.stateFlow.collectAsState().value) {
        is ReaderChapter.State.Error -> {
            Text("err")
        }
        is ReaderChapter.State.Loaded -> {
            LazyColumn {
                items(chapterState.pages) {
                    AsyncImage(
                        model = it.imageUrl,
                        contentDescription = null
                    )
                }
            }
        }
        ReaderChapter.State.Loading -> {
            Text("Loading")
        }
        ReaderChapter.State.Wait -> {

            Text("wait")
        }
    }
}