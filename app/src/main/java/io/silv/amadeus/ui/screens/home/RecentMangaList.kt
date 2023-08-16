package io.silv.amadeus.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.silv.amadeus.ui.composables.MangaListItem
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableManga
import kotlin.math.ceil
import kotlin.math.roundToInt

fun LazyListScope.recentMangaList(
    manga: LazyPagingItems<SavableManga>,
    onTagClick: (manga: SavableManga, name: String) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onMangaClick: (manga: SavableManga) -> Unit,
) {
    items(
        count = ceil(manga.itemCount / 2f).roundToInt(),
        contentType = manga.itemContentType(),
        key = manga.itemKey()
    ) {
        val space = LocalSpacing.current
        val items = listOfNotNull(manga[it * 2], manga[(it * 2) + 1])
        Row {
            for(item in items) {
                MangaListItem(
                    manga = item,
                    modifier = Modifier
                        .weight(1f)
                        .padding(space.large)
                        .clickable {
                            onMangaClick(item)
                        },
                    onTagClick = { name ->
                        onTagClick(item, name)
                    },
                    onBookmarkClick = {
                        onBookmarkClick(item)
                    }
                )
            }
        }
    }
}