package eu.kanade.tachiyomi.reader.model

import androidx.compose.runtime.Stable
import io.silv.common.model.Page
import java.io.InputStream


@Stable
open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    var stream: (() -> InputStream)? = null,
) : Page(index, url, imageUrl) {

    open lateinit var chapter: ReaderChapter
}
