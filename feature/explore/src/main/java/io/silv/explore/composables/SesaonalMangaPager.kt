package io.silv.explore.composables

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.silv.common.lerp
import io.silv.model.SavableManga
import io.silv.ui.CenterBox
import io.silv.ui.composables.TranslatedLanguageTags
import io.silv.ui.isLight
import io.silv.ui.theme.LocalSpacing
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeasonalMangaPager(
    modifier: Modifier,
    mangaList: ImmutableList<StateFlow<SavableManga>>,
    onMangaClick: (manga: SavableManga) -> Unit,
    onBookmarkClick: (manga: SavableManga) -> Unit,
    onTagClick: (name: String, id: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val space = LocalSpacing.current

    val pagerState =
        rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f,
            pageCount = {
                mangaList.size
            },
        )

    val onFirstPage by remember {
        derivedStateOf { pagerState.currentPage == 0 }
    }

    val onLastPage by remember {
        derivedStateOf { pagerState.currentPage == pagerState.pageCount }
    }

    fun animateScrollToPage(page: Int) {
        scope.launch {
            pagerState.animateScrollToPage(
                page,
                animationSpec = tween(500, easing = LinearOutSlowInEasing),
            )
        }
    }

    Column {
        Row(
            modifier =
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(space.xs),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = !onFirstPage,
                onClick = {
                    animateScrollToPage(0)
                },
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Arrow Back",
                )
            }
            AnimatedPageNumber(
                modifier = Modifier.weight(1f),
                otherPager = pagerState,
                onPageNumClick = ::animateScrollToPage,
            )
            IconButton(
                enabled = !onLastPage,
                onClick = {
                    animateScrollToPage(pagerState.pageCount)
                },
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Filled.ArrowForwardIos,
                    contentDescription = "Arrow Forward",
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            contentPadding = PaddingValues(space.small),
            pageSpacing = space.small,
            beyondBoundsPageCount = 2,
        ) { page ->

            val manga by mangaList[page].collectAsStateWithLifecycle()

            SeasonalPagingCard(
                pagerState = pagerState,
                page = page,
                manga = manga,
                onBookmarkClick = {
                    onBookmarkClick(manga)
                },
                onTagClick = {
                    manga.tagToId[it]?.let { id ->
                        onTagClick(it, id)
                    }
                },
                onClick = {
                    onMangaClick(manga)
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedPageNumber(
    modifier: Modifier = Modifier,
    otherPager: PagerState,
    onPageNumClick: (Int) -> Unit,
) {
    val state =
        rememberPagerState {
            otherPager.pageCount
        }

    LaunchedEffect(Unit) {
        snapshotFlow { otherPager.currentPage }.collect {
            state.animateScrollToPage(
                it,
                animationSpec = tween(500, easing = LinearOutSlowInEasing)
            )
        }
    }

    val pageSize = PageSize.Fixed(32.dp)
    val offset = pageSize.pageSize / 2

    BoxWithConstraints(
        modifier = modifier,
    ) {
        HorizontalPager(
            state = state,
            userScrollEnabled = false,
            pageSize = pageSize,
            contentPadding =
            PaddingValues(
                horizontal = (maxWidth / 2f) - offset,
            ),
        ) { page ->
            CenterBox(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable { onPageNumClick(page) }
                    .graphicsLayer {
                        val pageOffset =
                            (
                                (otherPager.currentPage - page) +
                                    otherPager
                                        .currentPageOffsetFraction
                                ).absoluteValue
                        val interpolation =
                            lerp(
                                start = 0.7f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f),
                            )
                        scaleX = interpolation
                        scaleY = interpolation
                        alpha = interpolation
                    },
            ) {
                val num = remember(page) { (page + 1).toString() }

                Text(
                    text = num,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SeasonalPagingCard(
    pagerState: PagerState,
    page: Int,
    manga: SavableManga,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
    onTagClick: (name: String) -> Unit,
) {
    val context = LocalContext.current
    val bg = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val imageLoader = context.imageLoader
    val space = LocalSpacing.current

    var dominantColor by remember { mutableStateOf(onSurface) }
    var vibrantColor by remember { mutableStateOf(onSurface) }

    LaunchedEffect(Unit) {
        try {
            val req =
                ImageRequest.Builder(context)
                    .data(manga)
                    .allowHardware(false) // Disable hardware bitmaps Palette cant get pixels
                    .build()

            val result = (imageLoader.execute(req) as SuccessResult).drawable
            val bitmap = (result as BitmapDrawable).bitmap
            val palette = Palette.from(bitmap).generate()

            val dominantColorInt =
                palette
                    .getDominantColor(dominantColor.toArgb())

            val vibrantColorInt =
                if (!bg.isLight()) {
                    palette.getLightVibrantColor(vibrantColor.toArgb())
                } else {
                    palette.getDarkVibrantColor(vibrantColor.toArgb())
                }

            dominantColor = Color(dominantColorInt)
            vibrantColor = Color(vibrantColorInt)
        } catch (e: Exception) {
            Log.e("MangaPager", e.stackTraceToString())
        }
    }
    Card(
        modifier =
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Calculate the absolute offset for the current page from the
                // scroll position. We use the absolute value which allows us to mirror
                // any effects for both directions
                val pageOffset =
                    (
                        (pagerState.currentPage - page) +
                            pagerState
                                .currentPageOffsetFraction
                        ).absoluteValue

                scaleX =
                    lerp(
                        start = 0.8f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f),
                    )
                scaleY =
                    lerp(
                        start = 0.8f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f),
                    )
                // We animate the alpha, between 50% and 100%
                alpha =
                    lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f),
                    )
            },
        colors =
        CardDefaults.outlinedCardColors(
            contentColor = vibrantColor,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier
                .fillMaxSize()
                .padding(space.med),
        ) {
            AsyncImage(
                model = manga,
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier =
                Modifier
                    .fillMaxHeight(0.9f)
                    .fillMaxWidth(0.4f),
            )
            Spacer(modifier = Modifier.width(space.med))
            Column(
                verticalArrangement = Arrangement.Top,
                modifier =
                Modifier
                    .fillMaxHeight(0.9f)
                    .weight(1f),
            ) {
                val textColor = vibrantColor

                Text(
                    text = manga.titleEnglish,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style =
                    MaterialTheme.typography.titleMedium.copy(
                        color = vibrantColor,
                    ),
                )
                Text(
                    text = manga.alternateTitles.getOrDefault("ja-ro", ""),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style =
                    MaterialTheme.typography.labelSmall.copy(
                        color = vibrantColor,
                    ),
                )
                TranslatedLanguageTags(tags = manga.availableTranslatedLanguages)
                Box(modifier = Modifier.weight(1f)) {
                    TagsGridWithBookMark(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        tags = remember(manga) { manga.tagToId.map { it.key } },
                        bookmarked = manga.inLibrary,
                        onBookmarkClick = onBookmarkClick,
                        onTagClick = onTagClick,
                        textColor = textColor,
                        iconTint = textColor,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsGridWithBookMark(
    modifier: Modifier = Modifier,
    tags: List<String>,
    bookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onTagClick: (tag: String) -> Unit = {},
    iconTint: Color = LocalContentColor.current,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val space = LocalSpacing.current

    Row(
        modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        CenterBox(Modifier.fillMaxHeight()) {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(space.xs),
                maxItemsInEachRow = ceil(tags.size / 2f).roundToInt(),
            ) {
                tags.fastForEach { tag ->
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
                        modifier =
                        Modifier
                            .padding(horizontal = space.xs)
                            .heightIn(0.dp, SuggestionChipDefaults.Height),
                    )
                }
            }
        }
    }
}
