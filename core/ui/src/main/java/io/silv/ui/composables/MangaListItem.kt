package io.silv.ui.composables

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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.silv.model.SavableManga
import io.silv.ui.CenterBox
import io.silv.ui.Language
import io.silv.ui.noRippleClickable
import io.silv.ui.shadow
import io.silv.ui.theme.LocalSpacing
import io.silv.ui.theme.Pastel
import io.silv.ui.theme.md_theme_dark_onSurface
import io.silv.ui.vertical
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
enum class CardType(val string: String) {
    List("List"),
    SemiCompact("Semi-Compact Card"),
    Compact("Compact Card"),
    ExtraCompact("Extra-Compact Card")
}


@Composable
fun MangaListItem(
    modifier: Modifier = Modifier,
    manga: SavableManga,
    onTagClick: (tag: String) -> Unit,
    onBookmarkClick: () -> Unit,
    cardType: CardType = CardType.Compact
) {

    val space = LocalSpacing.current

    Column(
        modifier = modifier
            .heightIn(
                min = 90.dp,
                max = (LocalConfiguration.current.screenHeightDp / 2).dp
            )
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(8.dp)
                )
        ) {
            AsyncImage(
                model = manga,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (cardType != CardType.ExtraCompact) {
                Text(
                    text = manga.titleEnglish,
                    maxLines = 2,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = md_theme_dark_onSurface,
                    modifier = Modifier
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = persistentListOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Black.copy(alpha = 0.9f)
                                        )
                                    )
                                )
                            }
                        }
                        .padding(
                            top = space.large,
                            bottom = space.med,
                            start = space.small,
                            end = space.small
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
        if (cardType == CardType.SemiCompact) {
            GenreTagsWithBookmark(
                tags = manga.tags,
                onBookmarkClick = onBookmarkClick,
                bookmarked = manga.bookmarked,
                modifier = Modifier.fillMaxWidth(),
                onTagClick = {
                    onTagClick(it)
                }
            )
        }
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
            CenterBox(
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
            tags =  manga.tags,
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
    tags: ImmutableList<String>,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                label = { Text(text = language, color = textColor) },
                modifier = Modifier.padding(horizontal = space.small),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = MaterialTheme.colorScheme.onBackground,
                    disabledLabelColor = MaterialTheme.colorScheme.onBackground
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    borderColor = textColor
                ),
            )
        }
    }
}

@Composable
fun TranslatedLanguageTags(
    modifier: Modifier = Modifier,
    tags: ImmutableList<String>,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Language.values().find { it.code == language }?.string ?: language
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
                    labelColor = textColor,
                    disabledLabelColor = textColor,
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    borderColor = textColor
                ),
                label = { Text(text, color = textColor) },
                modifier = Modifier.padding(horizontal = space.small)
            )
        }
    }
}

@Composable
fun GenreTagsWithBookmark(
    modifier: Modifier = Modifier,
    tags: ImmutableList<String>,
    bookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onTagClick: (tag: String) -> Unit = {},
    iconTint: Color = LocalContentColor.current,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val space = LocalSpacing.current

    LazyRow(modifier, verticalAlignment = Alignment.CenterVertically) {
        item {
            IconButton(onClick = onBookmarkClick) {
                if (bookmarked) Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = iconTint
                )
                else Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = iconTint
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
                    labelColor = textColor,
                    disabledLabelColor = textColor
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    borderColor = textColor
                ),
                modifier = Modifier.padding(horizontal = space.small)
            )
        }
    }
}
