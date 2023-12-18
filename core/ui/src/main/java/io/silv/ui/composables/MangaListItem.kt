package io.silv.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ElevatedSuggestionChip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.silv.model.SavableManga
import io.silv.ui.Language
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
enum class CardType(val string: String) {
    List("List"),
    SemiCompact("Semi-Compact Card"),
    Compact("Compact Card"),
    ExtraCompact("Extra-Compact Card"),
}

@Composable
fun MangaListItem(
    modifier: Modifier = Modifier,
    manga: SavableManga,
    onTagClick: (tag: String) -> Unit,
    onBookmarkClick: () -> Unit,
    cardType: CardType = CardType.Compact,
) {
    val space = LocalSpacing.current

    Column(
        modifier =
        modifier
            .heightIn(
                min = 90.dp,
                max = (LocalConfiguration.current.screenHeightDp / 2).dp,
            ),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(8.dp),
                ),
        ) {
            AsyncImage(
                model = manga,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (cardType != CardType.ExtraCompact) {
                Text(
                    text = manga.titleEnglish,
                    maxLines = 2,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFEDE0DD),
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    brush =
                                    Brush.verticalGradient(
                                        colors =
                                        persistentListOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Black.copy(alpha = 0.9f),
                                        ),
                                    ),
                                )
                            }
                        }
                        .padding(
                            top = space.large,
                            bottom = space.med,
                            start = space.small,
                            end = space.small,
                        )
                        .align(Alignment.BottomCenter),
                )
            }
        }
        if (cardType == CardType.SemiCompact) {
            GenreTagsWithBookmark(
                tags = manga.tags,
                onBookmarkClick = onBookmarkClick,
                bookmarked = manga.inLibrary,
                modifier = Modifier.fillMaxWidth(),
                onTagClick = {
                    onTagClick(it)
                },
            )
        }
    }
}

@Composable
fun MangaGenreTags(
    modifier: Modifier = Modifier,
    tags: ImmutableList<String>,
    onTagClick: (tag: String) -> Unit = {},
) {
    val space = LocalSpacing.current

    LazyRow(modifier) {
        items(
            items = tags,
            key = { item -> item },
        ) { language ->
            ElevatedSuggestionChip(
                onClick = { onTagClick(language) },
                label = { Text(text = language) },
                modifier = Modifier.padding(horizontal = space.small),
            )
        }
    }
}

@Composable
fun TranslatedLanguageTags(
    modifier: Modifier = Modifier,
    tags: ImmutableList<String>,
    onLanguageClick: (language: String) -> Unit = {},
) {
    val space = LocalSpacing.current

    LazyRow(modifier) {
        items(
            items = tags,
            key = { item -> item },
        ) { language ->

            var showLang by remember {
                mutableStateOf(false)
            }

            val text =
                remember(showLang) {
                    if (showLang) {
                        Language.values().find { it.code == language }?.string ?: language
                    } else {
                        language
                    }
                }

            ElevatedSuggestionChip(
                onClick = {
                    showLang = !showLang
                    onLanguageClick(language)
                },
                label = { Text(text) },
                modifier = Modifier.padding(horizontal = space.small),
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
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val space = LocalSpacing.current

    LazyRow(modifier, verticalAlignment = Alignment.CenterVertically) {
        item {
            IconButton(onClick = onBookmarkClick) {
                if (bookmarked) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = iconTint,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = iconTint,
                    )
                }
            }
        }
        items(
            items = tags,
            key = { item -> item },
        ) { tag ->
            SuggestionChip(
                onClick = { onTagClick(tag) },
                label = { Text(tag) },
                colors =
                SuggestionChipDefaults.suggestionChipColors(
                    labelColor = textColor,
                    disabledLabelColor = textColor,
                ),
                border =
                SuggestionChipDefaults.suggestionChipBorder(
                    borderColor = textColor,
                ),
                modifier = Modifier.padding(horizontal = space.small),
            )
        }
    }
}
