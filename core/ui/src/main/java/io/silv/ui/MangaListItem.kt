package io.silv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.model.SavableManga
import io.silv.ui.theme.LocalSpacing
import io.silv.ui.theme.Pastel

@Composable
fun MangaListItem(
    modifier: Modifier = Modifier,
    manga: SavableManga,
    onTagClick: (tag: String) -> Unit,
    onBookmarkClick: () -> Unit
) {

    val space = LocalSpacing.current

    Column(modifier.heightIn(min = 90.dp, max = (LocalConfiguration.current.screenHeightDp / 2).dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AsyncImage(
                model = manga,
                contentDescription = null,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(8.dp)
                    )
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()) {
                Spacer(modifier = Modifier.height(space.small))
                Spacer(modifier = Modifier.height(space.small))
                Text(
                    text = manga.titleEnglish,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.background
                    )
                )
            }

        }
        GenreTagsWithBookmark(
            tags = manga.tagToId.keys.toList(),
            onBookmarkClick = onBookmarkClick,
            bookmarked = manga.bookmarked,
            modifier = Modifier.fillMaxWidth(),
            onTagClick = {
                onTagClick(it)
            }
        )
    }
}

@Composable
fun MangaListItemSideTitle(
    modifier: Modifier = Modifier,
    index: Int,
    manga: SavableManga,
    onTagClick: (tag: String) -> Unit,
    onMangaImageClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {

    val context = LocalContext.current
    val space = LocalSpacing.current

    Column(modifier) {
        Row {
            Text(
                modifier = Modifier
                    .vertical()
                    .rotate(-90f)
                    .width(180.dp)
                    .align(Alignment.Top),
                text = "${index + 1} ${manga.titleEnglish}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            io.silv.ui.CenterBox(
                Modifier
                    .shadow(
                        color = Color.DarkGray,
                        blurRadius = 12.dp,
                        offsetY = space.xs,
                        offsetX = space.xs
                    )
                    .padding(space.xs)
                    .noRippleClickable { onMangaImageClick() }
            ) {
                val placeHolderColor = remember {
                    Pastel.getColorLight()
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(manga.coverArt)
                        .placeholder(placeHolderColor)
                        .error(placeHolderColor)
                        .fallback(placeHolderColor)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .clip(
                            RoundedCornerShape(12.dp)
                        )
                        .padding(space.xs),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(space.small))
        GenreTagsWithBookmark(
            tags =  manga.tagToId.keys.toList(),
            onBookmarkClick = onBookmarkClick,
            bookmarked = manga.bookmarked,
            modifier = Modifier.fillMaxWidth(),
            onTagClick = onTagClick
        )
    }
}

@Composable
fun MangaGenreTags(
    modifier: Modifier = Modifier,
    tags: List<String>,
    onTagClick: (tag: String) -> Unit = {}
) {
    val space = LocalSpacing.current

    LazyRow(modifier) {
        items(
            items = tags,
            key = { item -> item }
        ) {language ->
            SuggestionChip(
                onClick = { onTagClick(language) },
                label = { Text(language) },
                modifier = Modifier.padding(horizontal = space.small),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    disabledLabelColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}

@Composable
fun TranslatedLanguageTags(
    modifier: Modifier = Modifier,
    tags: List<String>,
    onLanguageClick: (language: String) -> Unit = {}
) {
    val space = LocalSpacing.current

    LazyRow(modifier) {
        items(
            items = tags,
            key = { item -> item }
        ) {language ->

            var showLang by remember {
                mutableStateOf(false)
            }

            val text = remember(showLang) {
                if (showLang) {
                    io.silv.ui.Language.values().find { it.code == language }?.string ?: language
                } else {
                    language
                }
            }

            SuggestionChip(
                onClick = {
                    showLang = !showLang
                    onLanguageClick(language)
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    disabledLabelColor = MaterialTheme.colorScheme.onBackground
                ),
                label = { Text(text) },
                modifier = Modifier.padding(horizontal = space.small)
            )
        }
    }
}

@Composable
fun GenreTagsWithBookmark(
    modifier: Modifier = Modifier,
    tags: List<String>,
    bookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onTagClick: (tag: String) -> Unit = {}
) {
    val space = LocalSpacing.current

    LazyRow(modifier, verticalAlignment = Alignment.CenterVertically) {
        item {
            IconButton(onClick = onBookmarkClick) {
                if (bookmarked) Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null
                )
                else Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null
                )
            }
        }
        items(
            items = tags,
            key = { item -> item }
        ) {tag ->
            SuggestionChip(
                onClick = { onTagClick(tag) },
                label = { Text(tag) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    disabledLabelColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(horizontal = space.small)
            )
        }
    }
}