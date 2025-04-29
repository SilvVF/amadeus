package io.silv.manga.stats

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.silv.common.DependencyAccessor
import io.silv.common.model.ReadingStatus
import io.silv.data.download.DownloadManager
import io.silv.di.dataDeps
import io.silv.di.downloadDeps
import io.silv.data.history.HistoryRepository
import io.silv.data.manga.interactor.GetLibraryMangaWithChapters
import io.silv.manga.R
import io.silv.ui.CenterBox
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class StatsScreenModel @OptIn(DependencyAccessor::class) constructor(
    private val downloadManager: DownloadManager = downloadDeps.downloadManager,
    private val historyRepository: HistoryRepository = dataDeps.historyRepository,
    private val getLibraryMangaWithChapters: GetLibraryMangaWithChapters = dataDeps.getLibraryMangaWithChapters,
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    init {
        screenModelScope.launch(Dispatchers.IO) {
            val libraryManga = getLibraryMangaWithChapters.await()

            val overviewStatData = StatsData.Overview(
                libraryMangaCount = libraryManga.size,
                completedMangaCount = libraryManga.count { it.manga.readingStatus == ReadingStatus.Completed },
                totalReadDuration = historyRepository.getTotalReadingTime()
            )


            val chaptersStatData = StatsData.Chapters(
                totalChapterCount = libraryManga.sumOf { it.chapters.size },
                readChapterCount = libraryManga.sumOf { manga -> manga.chapters.count { it.read } },
                downloadCount = downloadManager.getDownloadCount(),
            )

            mutableState.update {
                StatsScreenState.Success(
                    overview = overviewStatData,
                    chapters = chaptersStatData,
                )
            }
        }
    }

}

sealed interface StatsScreenState {
    @Immutable
    data object Loading : StatsScreenState

    @Immutable
    data class Success(
        val overview: StatsData.Overview,
        val chapters: StatsData.Chapters,
    ) : StatsScreenState
}

sealed interface StatsData {

    data class Overview(
        val libraryMangaCount: Int,
        val completedMangaCount: Int,
        val totalReadDuration: Long,
    ) : StatsData


    data class Chapters(
        val totalChapterCount: Int,
        val readChapterCount: Int,
        val downloadCount: Int,
    ) : StatsData
}

class StatsScreen: Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val screenModel = rememberScreenModel { StatsScreenModel() }
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    title = { Text("Stats") }
                )
            }
        ) { paddingValues ->
            when(val state = screenModel.state.collectAsStateWithLifecycle().value) {
                StatsScreenState.Loading -> {
                    CenterBox(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator()
                    }
                }
                is StatsScreenState.Success -> StatsScreenContent(state, paddingValues)
            }
        }
    }
}

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
) {
    val space = LocalSpacing.current
    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(space.small),
    ) {
        item {
            OverviewSection(state.overview)
        }
        item {
            ChapterStats(state.chapters)
        }
    }
}

@Composable
private fun LazyItemScope.ChapterStats(
    data: StatsData.Chapters,
) {
    SectionCard(R.string.chapters) {
        Row {
            StatsItem(
                data.totalChapterCount.toString(),
                stringResource(R.string.label_total_chapters),
            )
            StatsItem(
                data.readChapterCount.toString(),
                stringResource(R.string.label_read_chapters),
            )
            StatsItem(
                data.downloadCount.toString(),
                stringResource(R.string.label_downloaded),
            )
        }
    }
}

@Composable
fun LazyItemScope.SectionCard(
    titleRes: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val space = LocalSpacing.current
    if (titleRes != null) {
        Text(
            modifier = Modifier.padding(horizontal = space.xlarge),
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = space.med,
                vertical = space.small,
            ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(space.med)) {
            content()
        }
    }
}

@Composable
private fun LazyItemScope.OverviewSection(
    data: StatsData.Overview,
) {
    val context = LocalContext.current
    val readDurationString = remember(data.totalReadDuration) {
        data.totalReadDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = "")
    }
    SectionCard(R.string.label_overview_section) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StatsOverviewItem(
                title = data.libraryMangaCount.toString(),
                subtitle = stringResource(R.string.in_library),
                icon = Icons.Outlined.CollectionsBookmark,
            )
            StatsOverviewItem(
                title = readDurationString,
                subtitle = stringResource(R.string.label_read_duration),
                icon = Icons.Outlined.Schedule,
            )
            StatsOverviewItem(
                title = data.completedMangaCount.toString(),
                subtitle = stringResource(R.string.label_completed_titles),
                icon = Icons.Outlined.LocalLibrary,
            )
        }
    }
}

@Composable
fun RowScope.StatsOverviewItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    BaseStatsItem(
        title = title,
        titleStyle = MaterialTheme.typography.titleLarge,
        subtitle = subtitle,
        subtitleStyle = MaterialTheme.typography.bodyMedium,
        icon = icon,
    )
}

@Composable
fun RowScope.StatsItem(
    title: String,
    subtitle: String,
) {
    BaseStatsItem(
        title = title,
        titleStyle = MaterialTheme.typography.bodyMedium,
        subtitle = subtitle,
        subtitleStyle = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun RowScope.BaseStatsItem(
    title: String,
    titleStyle: TextStyle,
    subtitle: String,
    subtitleStyle: TextStyle,
    icon: ImageVector? = null,
) {
    val space = LocalSpacing.current
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(space.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = titleStyle
                .copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = subtitle,
            style = subtitleStyle
                .copy(
                    color = MaterialTheme.colorScheme.onSurface
                        .copy(alpha = 0.78f),
                ),
            textAlign = TextAlign.Center,
        )
        if (icon != null) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

fun Duration.toDurationString(context: Context, fallback: String): String {
    return toComponents { days, hours, minutes, seconds, _ ->
        buildList(4) {
            if (days != 0L) add(context.getString(R.string.day_short, days))
            if (hours != 0) add(context.getString(R.string.hour_short, hours))
            if (minutes != 0 && (days == 0L || hours == 0)) {
                add(
                    context.getString(R.string.minute_short, minutes),
                )
            }
            if (seconds != 0 && days == 0L && hours == 0) add(context.getString(R.string.seconds_short, seconds))
        }.joinToString(" ").ifBlank { fallback }
    }
}
