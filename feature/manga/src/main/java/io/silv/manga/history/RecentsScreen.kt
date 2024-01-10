package io.silv.manga.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import coil.compose.AsyncImage
import io.silv.common.model.MangaCover
import io.silv.common.time.localDateTimeNow
import io.silv.domain.history.HistoryWithRelations
import io.silv.ui.composables.SearchTopAppBar
import io.silv.ui.theme.AmadeusTheme
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

class RecentsScreen: Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<RecentsScreenModel>()

        val state by screenModel.state.collectAsStateWithLifecycle()

        RecentsScreenContent(
            state = state,
            searchText = screenModel.searchQuery,
            actions = RecentsActions(
                searchChanged = screenModel::searchChanged,
                clearHistory = screenModel::clearHistory
            )
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun RecentsScreenContent(
    state: RecentsState,
    searchText: String,
    actions: RecentsActions,
    preview: Boolean = false
) {
    val space = LocalSpacing.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            var searching by rememberSaveable { mutableStateOf(false) }
            SearchTopAppBar(
                title = "History",
                showTextField = searching,
                searchText = searchText,
                onSearchChanged = { searching = it},
                onSearchText = actions.searchChanged,
                onForceSearch = {},
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                actions = {
                    PlainTooltipBox(
                        tooltip = { Text("Clear history", color = MaterialTheme.colorScheme.onSurface) },
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    ) {
                        IconButton(
                            onClick = { /*TODO*/ },
                            modifier = Modifier.tooltipAnchor()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "delete all"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues
        ) {
            state.groupedByEpochDays.fastForEach { (epochDay, historyItems) ->
                stickyHeader {
                    val text = remember {
                        when(val dif = localDateTimeNow().date.toEpochDays() - epochDay) {
                            0 -> "Today"
                            1 -> "Yesterday"
                            else -> "$dif days ago"
                        }
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.labelLarge
                            .copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(space.med)
                    )
                }
                items(
                    items = historyItems,
                    key = { it.id }
                ) { history ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(space.med),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = MangaCover(
                                history.mangaId,
                                history.coverArt,
                                history.favorite,
                                history.mangaLastUpdatedAt
                            )
                                .takeIf { !preview } ?: history.coverArt,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(space.large))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = history.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 2
                            )
                            Text(
                                text = "Ch. ${history.chapterNumber} -" +
                                        " ${history.formattedTimeText}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(modifier = Modifier.width(space.large))
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewRecentsScreen() {
    AmadeusTheme {

        var searchText by remember{
            mutableStateOf("")
        }

        RecentsScreenContent(
            state = remember {
                RecentsState(
                    history = persistentListOf(
                        HistoryWithRelations(
                            id = 1,
                            chapterId="2c484989-75d8-4f17-ae42-beb620a717a2",
                            lastRead = localDateTimeNow(),
                            timeRead=11156,
                            mangaId= "296cbc31-af1a-4b5b-a34b-fee2b4cad542",
                            chapterName = "skdjfal",
                            title = "dkjfakljsdf",
                            lastPage = 1,
                            pageCount = 10,
                            chapterNumber = 1.0,
                            volume = 1,
                            favorite = false,
                            mangaLastUpdatedAt = 0L,
                            coverArt="https://uploads.mangadex.org/covers/296cbc31-af1a-4b5b-a34b-fee2b4cad542/8564d11b-1e50-4c60-91be-ea5ccb924996.jpg"
                        ),
                        HistoryWithRelations(
                            id = 2,
                            chapterId="2c484989-75d8-4f17-ae42-beb620a717a2",
                            lastRead = LocalDateTime.parse("2023-10-05T14:48"),
                            timeRead=11156,
                            mangaId= "296cbc31-af1a-4b5b-a34b-fee2b4cad542",
                            chapterName = "skdjfalasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf",
                            title = "skdjfalasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf",
                            lastPage = 1,
                            pageCount = 10,
                            chapterNumber = 1.0,
                            volume = 1,
                            favorite = false,
                            mangaLastUpdatedAt = 0L,
                            coverArt="https://uploads.mangadex.org/covers/296cbc31-af1a-4b5b-a34b-fee2b4cad542/8564d11b-1e50-4c60-91be-ea5ccb924996.jpg"
                        ),
                        HistoryWithRelations(
                            id = 3,
                            chapterId="2c484989-75d8-4f17-ae42-beb620a717a2",
                            lastRead = localDateTimeNow(),
                            timeRead=11156,
                            mangaId= "296cbc31-af1a-4b5b-a34b-fee2b4cad542",
                            chapterName = "skdjfal",
                            title = "dkjfakljsdf",
                            lastPage = 1,
                            pageCount = 10,
                            chapterNumber = 1.0,
                            volume = 1,
                            favorite = false,
                            mangaLastUpdatedAt = 0L,
                            coverArt="https://uploads.mangadex.org/covers/296cbc31-af1a-4b5b-a34b-fee2b4cad542/8564d11b-1e50-4c60-91be-ea5ccb924996.jpg"
                        )
                    )
                )
            },
            preview = true,
            searchText = searchText,
            actions = RecentsActions(
                searchChanged = {
                    searchText =it
                }
            )
        )
    }
}