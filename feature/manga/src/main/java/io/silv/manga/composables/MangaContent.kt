package io.silv.manga.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.Material3RichText
import io.silv.common.model.ReadingStatus
import io.silv.data.manga.model.Manga
import io.silv.ui.composables.TranslatedLanguageTags
import io.silv.ui.noRippleClickable
import io.silv.ui.theme.LocalSpacing


import kotlin.math.roundToInt

@Composable
fun ColumnScope.MangaDescription(
    manga: Manga,
    onTagSelected: (tag: String) -> Unit,
) {
    val space = LocalSpacing.current
    MangaInfo(
        manga = manga,
        onTagSelected = onTagSelected,
    )
}

@Stable
private data class MangaActionItem(
    val icon: ImageVector,
    val label: String,
    val action: () -> Unit,
    val selected: Boolean = false,
)

@Composable
fun MangaActions(
    modifier: Modifier,
    readingStatus: ReadingStatus,
    inLibrary: Boolean,
    showChapterArt: () -> Unit,
    addToLibraryClicked: () -> Unit,
    changeStatus: (ReadingStatus) -> Unit
) {
    val space = LocalSpacing.current
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        var statusDialogVisible by remember {
            mutableStateOf(false)
        }

        val items = remember(inLibrary) {
            listOf(
                MangaActionItem(
                    icon = if (inLibrary) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = if (inLibrary) "Added to library" else "Add to library",
                    selected = inLibrary,
                    action = addToLibraryClicked,
                ),
                MangaActionItem(Icons.Filled.Image, "Cover art", showChapterArt),
            )
        }

        items.fastForEach { (icon, label, action, selected) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                IconButton(
                    onClick = action,
                    modifier = Modifier.padding(horizontal = space.large),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = icon.name,
                        tint = color,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(color = color),
                )
            }
        }
        Box {

            val statuses = remember { ReadingStatus.entries.toList() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = { statusDialogVisible = !statusDialogVisible },
                    modifier = Modifier.padding(horizontal = space.large),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CollectionsBookmark,
                        contentDescription = "reading status",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = "Reading status",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                )
            }
            DropdownMenu(
                expanded = statusDialogVisible,
                offset = DpOffset(x = -(space.med), y = 0.dp),
                onDismissRequest = { statusDialogVisible = false }
            ) {
                statuses.fastForEach { status ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = status == readingStatus,
                            onClick = { changeStatus(status) }
                        )
                        DropdownMenuItem(
                            text = { Text(status.toString()) },
                            onClick = { changeStatus(status) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagsAndLanguages(
    manga: Manga,
    navigate: (name: String) -> Unit,
) {
    val space = LocalSpacing.current
    val tags = remember(manga) { manga.tagToId.keys.toList() }

    Text(
        text = "Tags",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(space.med),
    )
    Column(Modifier.animateContentSize()) {
        LazyRow {
            items(
                tags,
                key = { item -> item },
            ) { tag ->
                ElevatedSuggestionChip(
                    onClick = { navigate(tag) },
                    label = { Text(tag) },
                    modifier = Modifier.padding(horizontal = space.small),
                )
            }
        }
        Text(
            text = "Translated Languages",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(space.med),
        )
        TranslatedLanguageTags(
            tags = remember(manga.id) {
                manga.availableTranslatedLanguages.toList()
            }
        )
    }
}

private val whitespaceLineRegex = Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE))

@Composable
private fun MangaInfo(
    manga: Manga,
    onTagSelected: (tag: String) -> Unit,
) {
    var expanded by rememberSaveable {
        mutableStateOf(false)
    }
    val space = LocalSpacing.current
    TagsAndLanguages(
        manga = manga,
        navigate = onTagSelected,
    )
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(space.med),
        )
        MangaSummary(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp)
                    .noRippleClickable { expanded = !expanded },
            expandedDescription = manga.description,
            shrunkDescription =
                remember(manga.description) {
                    manga.description.replace(whitespaceLineRegex, "\n")
                        .trimEnd()
                },
            expanded = expanded,
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
    var expandedHeight by remember { mutableIntStateOf(0) }
    var shrunkHeight by remember { mutableIntStateOf(0) }
    val heightDelta = remember(expandedHeight, shrunkHeight) { expandedHeight - shrunkHeight }
    val animProgress by animateFloatAsState(if (expanded) 1f else 0f, label = "progress")
    val scrimHeight = with(LocalDensity.current) { remember { 24.sp.roundToPx() } }

    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        val shrunkPlaceable =
            subcompose("description-s") {
                Text(
                    text = "\n\n", // Shows at least 3 lines
                    style = MaterialTheme.typography.bodyMedium,
                )
            }.map { it.measure(constraints) }
        shrunkHeight = shrunkPlaceable.maxByOrNull { it.height }?.height ?: 0

        val expandedPlaceable =
            subcompose("description-l") {
                Material3RichText(
                    modifier = Modifier.alpha(0.78f),
                ) {
                    Markdown(content = expandedDescription)
                }
            }.map { it.measure(constraints) }

        expandedHeight = expandedPlaceable.maxByOrNull { it.height }?.height?.coerceAtLeast(
            shrunkHeight
        ) ?: 0

        val actualPlaceable =
            subcompose("description") {
                SelectionContainer {
                    if (expanded) {
                        Material3RichText(
                            modifier = Modifier.alpha(0.78f),
                        ) {
                            Markdown(content = expandedDescription)
                        }
                    } else {
                        Text(
                            shrunkDescription,
                            maxLines = Int.MAX_VALUE,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.alpha(0.78f),
                        )
                    }
                }
            }.map { it.measure(constraints) }

        val scrimPlaceable =
            subcompose("scrim") {
                val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                Box(
                    modifier = Modifier.background(Brush.verticalGradient(colors = colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector =
                            if (expanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.background(
                            Brush.radialGradient(colors = colors.asReversed())
                        ),
                    )
                }
            }.map {
                it.measure(
                    Constraints.fixed(width = constraints.maxWidth, height = scrimHeight)
                )
            }

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
