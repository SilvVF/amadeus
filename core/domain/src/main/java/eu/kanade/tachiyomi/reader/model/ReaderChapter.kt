package eu.kanade.tachiyomi.reader.model

import androidx.compose.runtime.Stable
import eu.kanade.tachiyomi.reader.loader.PageLoader
import io.silv.model.SavableChapter
import kotlinx.coroutines.flow.MutableStateFlow

@Stable
data class ReaderChapter(
    val chapter: SavableChapter
) {

    val stateFlow = MutableStateFlow<State>(State.Wait)
    var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    val pages: List<ReaderPage>?
        get() = (state as? State.Loaded)?.pages

    var pageLoader: PageLoader? = null

    var requestedPage: Int = 0

    private var references = 0

    fun ref() {
        references++
    }

    fun unref() {
        references--
        if (references == 0) {
            pageLoader?.recycle()
            pageLoader = null
            state = State.Wait
        }
    }

    sealed interface State {
        data object Wait : State
        data object Loading : State
        data class Error(val error: Throwable) : State
        data class Loaded(val pages: List<ReaderPage>) : State
    }
}