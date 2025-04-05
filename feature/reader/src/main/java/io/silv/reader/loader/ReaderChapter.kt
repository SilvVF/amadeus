package io.silv.reader.loader

import androidx.compose.runtime.Stable
import io.silv.common.model.ChapterResource
import io.silv.domain.chapter.model.Chapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime

@Stable
data class ReaderChapter(
    var chapter: Chapter
) {
    private val stateFlow = MutableStateFlow<State>(State.Wait)

    fun updateChapter(
        id: String = this.chapter.id,
        url: String = this.chapter.url,
        bookmarked: Boolean = this.chapter.bookmarked,
        downloaded: Boolean = this.chapter.downloaded,
        mangaId: String = this.chapter.mangaId,
        title: String = this.chapter.title,
        volume: Int = this.chapter.volume,
        chapter: Double = this.chapter.chapter,
        pages: Int = this.chapter.pages,
        lastReadPage: Int? = this.chapter.lastReadPage,
        translatedLanguage: String = this.chapter.translatedLanguage,
        uploader: String = this.chapter.uploader,
        scanlationGroupToId: Pair<String, String>? = this.chapter.scanlationGroupToId,
        userToId: Pair<String, String>? = this.chapter.userToId,
        version: Int = this.chapter.version,
        createdAt: LocalDateTime = this.chapter.createdAt,
        updatedAt: LocalDateTime = this.chapter.updatedAt,
        readableAt: LocalDateTime = this.chapter.readableAt,
        ableToDownload: Boolean = this.chapter.ableToDownload
    ) {
        this.chapter = Chapter(
            id, url, bookmarked, downloaded, mangaId, title, volume, chapter, pages,
            lastReadPage, translatedLanguage, uploader, scanlationGroupToId,
            userToId, version, createdAt, updatedAt, readableAt, ableToDownload
        )
    }

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
