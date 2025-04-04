package io.silv.common.model

import androidx.compose.runtime.Stable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.sample


@Stable
data class Download(
    val manga: MangaResource,
    val chapter: ChapterResource,
) {
    var pages: List<Page>? = null

    val totalProgress: Int
        get() = pages?.sumOf(Page::progress) ?: 0

    val downloadedImages: Int
        get() = pages?.count { it.status == Page.State.READY } ?: 0

    @OptIn(FlowPreview::class)
    @Transient
    val progressFlow =
        flow {
            if (pages == null) {
                emit(0)
                while (pages == null) {
                    delay(50)
                }
            }

            emitAll(combine(pages!!.map(Page::progressFlow)) { it.average().toInt() })
        }
            .distinctUntilChanged()
            .sample(50)

    val progress: Int
        get() {
            val pages = pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }
}
