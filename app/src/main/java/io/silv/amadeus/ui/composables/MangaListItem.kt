package io.silv.amadeus.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Stadium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.manga.domain.models.DomainManga
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.shared.shadow
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.local.entity.MangaResource

@Composable
fun MangaListItem(
    modifier: Modifier = Modifier,
    manga: DomainManga,
    onBookmarkClick: () -> Unit
) {

    val context = LocalContext.current
    val space = LocalSpacing.current

    Column(modifier) {
        CenterBox(
            modifier = Modifier
                .shadow(
                    color = Color.DarkGray,
                    blurRadius = 12.dp,
                    offsetY = space.xs,
                    offsetX = space.xs
                )
                .padding(space.xs)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(manga.coverArt)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .clip(
                        RoundedCornerShape(12.dp)
                    ),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(space.small))
        TranslatedLanguageTags(
            tags = manga.availableTranslatedLanguages,
            onBookmarkClick = onBookmarkClick,
            bookmarked = manga.bookmarked,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(space.small))
        Text(
            text = manga.titleEnglish,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun TranslatedLanguageTags(
    modifier: Modifier = Modifier,
    tags: List<String>,
    bookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onLanguageClick: (language: String) -> Unit = {}
) {
    val space = LocalSpacing.current

    LazyRow(modifier) {
        item {
            IconButton(onClick = onBookmarkClick) {
                if (bookmarked) Icon(
                    imageVector = Icons.Filled.BookmarkRemove,
                    contentDescription = null
                )
                else Icon(
                    imageVector = Icons.Outlined.BookmarkAdd,
                    contentDescription = null
                )
            }
        }
        items(
            items = tags,
            key = { item -> item }
        ) {language ->
            SuggestionChip(
                onClick = { onLanguageClick(language) },
                label = { Text(language) },
                modifier = Modifier.padding(space.small)
            )
        }
    }
}