package io.silv.amadeus.ui.screens.manga_view.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.silv.amadeus.ui.composables.TranslatedLanguageTags
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.manga.domain.models.SavableManga
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsAndLanguages(
    manga: SavableManga,
    navigate: (name: String) -> Unit,
) {
    val space = LocalSpacing.current
    val list = remember(manga) {
        manga.tagToId.keys.toList()
    }
    var expanded by rememberSaveable {
        mutableStateOf(list.size < 4)
    }
    Text("Tags", style = MaterialTheme.typography.labelSmall)
    Column(Modifier.animateContentSize()) {
        FlowRow {
            if (!expanded) {
                list.take(3).forEach {name ->
                    AssistChip(
                        onClick = { navigate(name)},
                        label = { Text(name) },
                        modifier = Modifier.padding(horizontal = space.xs)
                    )
                }
                if (list.size > 4) {
                    AssistChip(
                        onClick = { expanded = true },
                        label = { Text("+ ${list.size - 3} more") },
                        modifier = Modifier.padding(horizontal = space.xs)
                    )
                }
            } else {
                list.forEach { name ->
                    AssistChip(
                        onClick = { navigate(name) },
                        label = { Text(name) },
                        modifier = Modifier.padding(horizontal = space.xs)
                    )
                }
                if (list.size > 4) {
                    IconButton(onClick = { expanded = false }) {
                        Icon(imageVector = Icons.Filled.KeyboardArrowLeft, contentDescription = null)
                    }
                }
            }
        }
        Text("Translated Languages", style = MaterialTheme.typography.labelSmall)
        TranslatedLanguageTags(tags = manga.availableTranslatedLanguages)
    }
}

@Composable
fun MangaContent(
    manga: SavableManga,
    bookmarked: Boolean,
    onBookmarkClicked: (String) -> Unit,
    onTagSelected: (tag: String) -> Unit,
    viewOnWebClicked: () -> Unit,
) {
    val space = LocalSpacing.current
    Column(Modifier.padding(horizontal = space.med)) {
        MangaActions(
            manga = manga,
            bookmarked = bookmarked,
            onBookmarkClicked = onBookmarkClicked,
            viewOnWebClicked = viewOnWebClicked
        )
        MangaInfo(
            manga = manga,
            onTagSelected = onTagSelected
        )
    }
}

@Composable
private fun MangaActions(
    manga: SavableManga,
    bookmarked: Boolean,
    onBookmarkClicked: (String) -> Unit,
    viewOnWebClicked: () -> Unit,
) {
    val space = LocalSpacing.current
    Row(
        Modifier
            .padding(vertical = space.med)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { onBookmarkClicked(manga.id) },
                modifier = Modifier.padding(horizontal = space.large)
            ) {
                Icon(
                    imageVector = if (bookmarked) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = null,
                    tint = if(bookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = if (bookmarked) "Added to library" else "Add to library",
                style = MaterialTheme.typography.labelMedium
                    .copy(
                        color = if(bookmarked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground
                    )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = viewOnWebClicked,
                modifier = Modifier.padding(horizontal = space.large)
            ) {
                Icon(
                    imageVector = Icons.Filled.TravelExplore,
                    contentDescription = null
                )
            }
            Text(text = "View on web", style = MaterialTheme.typography.labelMedium)
        }
    }
}


private val whitespaceLineRegex = Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE))

@Composable
private fun MangaInfo(
    manga: SavableManga,
    onTagSelected: (tag: String) -> Unit
) {
    var expanded by rememberSaveable {
        mutableStateOf(false)
    }
    TagsAndLanguages(
        manga = manga,
        navigate = onTagSelected
    )
    Column(
        Modifier
    ) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.labelSmall
        )
        MangaSummary(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .noRippleClickable { expanded = !expanded },
            expandedDescription = manga.description,
            shrunkDescription =  remember(manga.description) {
                manga.description.replace(whitespaceLineRegex, "\n")
                .trimEnd()
            },
            expanded = expanded
        )
    }
}

@Composable
private fun MangaSummary(
    expandedDescription: String,
    shrunkDescription: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    var expandedHeight by remember { mutableStateOf(0) }
    var shrunkHeight by remember { mutableStateOf(0) }
    val heightDelta = remember(expandedHeight, shrunkHeight) { expandedHeight - shrunkHeight }
    val animProgress by animateFloatAsState(if (expanded) 1f else 0f, label = "progress")
    val scrimHeight = with(LocalDensity.current) { remember { 24.sp.roundToPx() } }

    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        val shrunkPlaceable = subcompose("description-s") {
            Text(
                text = "\n\n", // Shows at least 3 lines
                style = MaterialTheme.typography.bodyMedium,
            )
        }.map { it.measure(constraints) }
        shrunkHeight = shrunkPlaceable.maxByOrNull { it.height }?.height ?: 0

        val expandedPlaceable = subcompose("description-l") {
            Text(
                text = expandedDescription,
                style = MaterialTheme.typography.bodyMedium,
            )
        }.map { it.measure(constraints) }
        expandedHeight = expandedPlaceable.maxByOrNull { it.height }?.height?.coerceAtLeast(shrunkHeight) ?: 0

        val actualPlaceable = subcompose("description") {
            SelectionContainer {
                Text(
                    text = if (expanded) expandedDescription else shrunkDescription,
                    maxLines = Int.MAX_VALUE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.alpha(0.78f),
                )
            }
        }.map { it.measure(constraints) }

        val scrimPlaceable = subcompose("scrim") {
            val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
            Box(
                modifier = Modifier.background(Brush.verticalGradient(colors = colors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if(expanded)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.background(Brush.radialGradient(colors = colors.asReversed())),
                )
            }
        }.map { it.measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeight)) }

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeight) * animProgress).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.forEach {
                it.place(0, 0)
            }

            val scrimY = currentHeight - scrimHeight
            scrimPlaceable.forEach {
                it.place(0, scrimY)
            }
        }
    }
}
