package eu.kanade.tachiyomi.reader.loader

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.MangaDexSource
import eu.kanade.tachiyomi.reader.DownloadManager
import eu.kanade.tachiyomi.reader.model.Page
import eu.kanade.tachiyomi.reader.model.ReaderChapter
import eu.kanade.tachiyomi.reader.model.ReaderPage
import io.silv.model.SavableManga
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class DownloadPageLoader(
    private val chapter: ReaderChapter,
    private val manga: SavableManga,
    private val downloadManager: DownloadManager,
) : PageLoader(), KoinComponent {

    private val source = MangaDexSource
    private val context by inject<Context>()

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        return getPagesFromDirectory()
    }


    private fun getPagesFromDirectory(): List<ReaderPage> {
        val pages = downloadManager.buildPageList(source, manga, chapter.chapter)
        return pages.map { page ->
            ReaderPage(page.index, page.url, page.imageUrl) {
                context.contentResolver.openInputStream(page.imageUrl?.toUri() ?: Uri.EMPTY)!!
            }.apply {
                status = Page.State.READY
            }
        }
    }

    override suspend fun loadPage(page: ReaderPage) = Unit
}