package io.silv.ui.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ripple
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.silv.data.manga.model.Manga
import io.silv.ui.Language
import io.silv.ui.theme.LocalSpacing
import io.silv.ui.tryApplySharedElementTransition


@Stable
enum class CardType(val string: String) {
    SemiCompact("Semi-Compact Card"),
    Compact("Compact Card"),
    ExtraCompact("Extra-Compact Card"),
}


@Composable
fun MangaListItem(
    modifier: Modifier = Modifier,
    manga: Manga,
    onClick: (manga: Manga) -> Unit = {},
    onFavoriteClick: () -> Unit,
) {
    val space = LocalSpacing.current
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onLongClick = {
                    onFavoriteClick()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onClick = {
                    onClick(manga)
                }
            )
            .padding(space.med),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = manga.takeIf { !it.isStub } ?: manga.coverArt,
            contentDescription = null,
            modifier = Modifier
                .tryApplySharedElementTransition(
                    key = { manga.id }
                )
                .clip(RoundedCornerShape(8.dp),)
                .fillMaxWidth(0.13f)
                .aspectRatio(1f / 1f),
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(Color(0x1F888888))
        )
        Spacer(modifier = Modifier.width(space.med))
        Text(
            text = manga.titleEnglish,
            maxLines = 2,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .tryApplySharedElementTransition(
                    key = { manga.titleEnglish },
                )
                .weight(1f),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
        )
        if (manga.inLibrary) {
            val primaryContainer = MaterialTheme.colorScheme.primaryContainer
            Text(
                text = "In Library",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.drawWithCache {
                    onDrawBehind {
                        drawRoundRect(
                            color = primaryContainer,
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
                    .padding(space.xs)

            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MangaGridItem(
    modifier: Modifier = Modifier,
    manga: Manga,
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
                model = manga.takeIf { !it.isStub } ?: manga.coverArt,
                contentDescription = null,
                modifier = Modifier
                    .tryApplySharedElementTransition(
                        key = { manga.id }
                    )
                    .fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(Color(0x1F888888))
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
                        .tryApplySharedElementTransition(
                            key = { manga.titleEnglish }
                        )
                        .fillMaxWidth()
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    brush =
                                    Brush.verticalGradient(
                                        colors =
                                        listOf(
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
fun TranslatedLanguageTags(
    modifier: Modifier = Modifier,
    tags: List<String>,
    onLanguageClick: (language: String) -> Unit = {},
) {
    val space = LocalSpacing.current

    LazyRow(modifier) {
        items(
            items = tags,
            key = { item -> item },
        ) { language ->

            var showLang by remember { mutableStateOf(false) }

            val text =
                remember(showLang) {
                    if (showLang) {
                        Language.entries.find { it.code == language }?.string ?: language
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
    tags: List<String>,
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
                    enabled = true,
                    borderColor = textColor,
                ),
                modifier = Modifier.padding(horizontal = space.small),
            )
        }
    }
}
