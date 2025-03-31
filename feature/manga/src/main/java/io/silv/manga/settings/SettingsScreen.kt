package io.silv.manga.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.common.model.AppTheme
import io.silv.common.model.AutomaticUpdatePeriod
import io.silv.datastore.ExplorePrefs
import io.silv.datastore.FilterPrefs
import io.silv.datastore.LibraryPrefs
import io.silv.datastore.ReaderPrefs
import io.silv.datastore.UserSettings
import io.silv.datastore.collectAsState
import io.silv.ui.Converters
import io.silv.ui.ReaderLayout
import io.silv.ui.composables.CardType
import io.silv.ui.composables.SelectCardType
import io.silv.ui.composables.UseList
import io.silv.ui.theme.LocalSpacing
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

class SettingsScreen: Screen {

    @Composable
    override fun Content() {

        val screenModel = getScreenModel<SettingsScreenModel>()

        val settings by screenModel.settingsPrefs.collectAsStateWithLifecycle()
        val state by screenModel.state.collectAsStateWithLifecycle()

        SettingsScreenContent(
            settings = settings,
            state = state,
            actions = SettingsActions(
                changeCurrentDialog = screenModel::changeCurrentDialog,
                changeUpdatePeriod = screenModel::changeAutomaticUpdatePeriod,
                changeTheme = screenModel::changeAppTheme
            )
        )
    }
}

@Stable
data class SettingsActions(
    val changeCurrentDialog: (String?) -> Unit,
    val changeUpdatePeriod: (AutomaticUpdatePeriod) -> Unit,
    val changeTheme: (AppTheme) -> Unit
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreenContent(
    state: SettingsState,
    settings: UserSettings,
    actions: SettingsActions
) {

    val navigator = LocalNavigator.current
    val space = LocalSpacing.current

    when (state.dialogkey) {
        SettingsScreenModel.UPDATE_PERIOD_KEY -> {
            BasicAlertDialog(
                onDismissRequest = { actions.changeCurrentDialog(null) },
                properties = DialogProperties(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shadowElevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                ){
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start,
                        modifier =  Modifier.padding(space.xlarge)
                    ) {
                        Text("Automatic updates", style = MaterialTheme.typography.titleLarge)
                        AutomaticUpdatePeriod.entries.fastForEach {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.padding(space.med)
                            ) {
                                RadioButton(
                                    selected = settings.updateInterval == it,
                                    onClick = { actions.changeUpdatePeriod(it) }
                                )
                                Text(it.toString())
                            }
                        }
                    }
                }
            }
        }
        SettingsScreenModel.LIBRARY_DISPLAY_KEY -> {
            val scope = rememberCoroutineScope()
            val cardType = LibraryPrefs.cardTypePrefKey.collectAsState(
                defaultValue = CardType.Compact,
                converter = Converters.CardTypeToStringConverter,
                scope = scope,
            )
            val gridCells = LibraryPrefs.gridCellsPrefKey.collectAsState(
                LibraryPrefs.gridCellsDefault,
                scope
            )
            val useList = LibraryPrefs.useListPrefKey.collectAsState(false, scope)
            val animatePlacement = LibraryPrefs.animatePlacementPrefKey.collectAsState(
                true,
                scope
            )
            DisplayPrefsDialogContent(
                cardType = cardType,
                useList = useList,
                gridCells = gridCells,
                animatePlacement = animatePlacement,
                onDismiss = { actions.changeCurrentDialog(null) },
                title = "Library display options"
            )
        }
        SettingsScreenModel.EXPLORE_DISPLAY_KEY -> {
            val scope = rememberCoroutineScope()
            val cardType = ExplorePrefs.cardTypePrefKey.collectAsState(
                defaultValue = CardType.Compact,
                converter = Converters.CardTypeToStringConverter,
                scope = scope,
            )
            val gridCells = ExplorePrefs.gridCellsPrefKey.collectAsState(
                LibraryPrefs.gridCellsDefault,
                scope
            )
            val useList = ExplorePrefs.useListPrefKey.collectAsState(false, scope)
            DisplayPrefsDialogContent(
                cardType = cardType,
                useList = useList,
                gridCells = gridCells,
                animatePlacement = null,
                onDismiss = { actions.changeCurrentDialog(null) },
                title = "Explore display options"
            )
        }
        SettingsScreenModel.FILTER_DISPLAY_KEY -> {
            val scope = rememberCoroutineScope()
            val cardType = FilterPrefs.cardTypePrefKey.collectAsState(
                defaultValue = CardType.Compact,
                converter = Converters.CardTypeToStringConverter,
                scope = scope,
            )
            val gridCells = FilterPrefs.gridCellsPrefKey.collectAsState(
                FilterPrefs.gridCellsDefault,
                scope
            )
            val useList = FilterPrefs.useListPrefKey.collectAsState(false, scope)
            DisplayPrefsDialogContent(
                cardType = cardType,
                useList = useList,
                gridCells = gridCells,
                animatePlacement = null,
                onDismiss = { actions.changeCurrentDialog(null) },
                title = "Filter display options"
            )
        }
        null -> Unit
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "Settings",
                onBackClicked = { navigator?.pop() }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .78f)
                    ),
                    modifier = Modifier.padding(horizontal = space.large)
                )
                FlowRow(
                    verticalArrangement = Arrangement.Center
                ) {
                    AppTheme.entries.fastForEach {
                        FilterChip(
                            selected = settings.theme == it,
                            onClick = { actions.changeTheme(it) },
                            label = { Text(it.toString()) },
                            modifier = Modifier.padding(space.small)
                        )
                    }
                }
            }
            Text(
                "Global update",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .78f)
                ),
                modifier = Modifier.padding(horizontal = space.large)
            )
            Spacer(modifier = Modifier.height(space.large))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { actions.changeCurrentDialog(SettingsScreenModel.UPDATE_PERIOD_KEY) }
                    .padding(space.large),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text("Automatic updates", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(space.small))
                Text(
                    "${settings.updateInterval}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .78f)
                    )
                )
            }
            HorizontalDivider()
            Text(
                "Display options",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .78f)
                ),
                modifier = Modifier
                    .padding(horizontal = space.large)
                    .padding(top = space.large)
            )
            Spacer(modifier = Modifier.height(space.large))
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                SettingsItem(
                    title = "Library",
                    description = "library display options",
                    icon = Icons.Filled.CollectionsBookmark) {
                    actions.changeCurrentDialog(SettingsScreenModel.LIBRARY_DISPLAY_KEY)
                }
                SettingsItem(
                    title = "Explore",
                    description = "explore display options",
                    icon = Icons.Filled.Explore) {
                    actions.changeCurrentDialog(SettingsScreenModel.EXPLORE_DISPLAY_KEY)
                }
                SettingsItem(
                    title = "Filter",
                    description = "filter display options",
                    icon = Icons.Filled.AccessTime) {
                    actions.changeCurrentDialog(SettingsScreenModel.FILTER_DISPLAY_KEY)
                }
            }
            HorizontalDivider()
            Text(
                "Reader options",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .78f)
                ),
                modifier = Modifier
                    .padding(horizontal = space.large)
                    .padding(top = space.large)
            )
            Spacer(modifier = Modifier.height(space.large))
            ReaderOptions(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderOptions(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val space = LocalSpacing.current

    var fullscreen by ReaderPrefs.fullscreen.collectAsState(true, scope)
    var showPageNumber by ReaderPrefs.showPageNumber.collectAsState(true, scope)
    var layout by ReaderPrefs.layoutDirection.collectAsState(
        defaultValue = ReaderLayout.PagedRTL,
        converter = Converters.LayoutDirectionConverter,
        scope
    )
    var backgroundColor by ReaderPrefs.backgroundColor.collectAsState(3, scope)

    Column(
        modifier.padding(space.med),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Reading mode")
            FlowRow(
                verticalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = layout == ReaderLayout.PagedLTR,
                    onClick = { layout = ReaderLayout.PagedLTR },
                    label = { Text("Paged (left to right)") },
                    modifier = Modifier.padding(space.small)
                )
                FilterChip(
                    selected = layout == ReaderLayout.PagedRTL,
                    onClick = { layout = ReaderLayout.PagedRTL },
                    label = { Text("Paged (right to left)") },
                    modifier = Modifier.padding(space.small)
                )
                FilterChip(
                    selected = layout == ReaderLayout.Vertical,
                    onClick = { layout = ReaderLayout.Vertical },
                    label = { Text("Vertical") },
                    modifier = Modifier.padding(space.small)
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Background color")
            FlowRow(
                verticalArrangement = Arrangement.Center
            ) {
                val colors = remember {
                    persistentListOf(
                        Color.Black to "Black",
                        Color.Gray to "Gray",
                        Color.White to "White",
                        Color.Unspecified to "Default"
                    )
                }
                colors.fastForEachIndexed { i, c ->
                    FilterChip(
                        selected = backgroundColor == i,
                        onClick = { backgroundColor = i },
                        label = { Text(c.second) },
                        modifier = Modifier.padding(space.small)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = fullscreen, onCheckedChange = { fullscreen = it })
            Text("Fullscreen")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showPageNumber, onCheckedChange = { showPageNumber = it })
            Text("Show page number")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayPrefsDialogContent(
    modifier: Modifier = Modifier,
    title: String,
    cardType:  MutableState<CardType>,
    gridCells: MutableState<Int>,
    useList: MutableState<Boolean>,
    animatePlacement: MutableState<Boolean>?,
    onDismiss: () -> Unit
) {
    val space = LocalSpacing.current
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        ) {
            Column(
                Modifier
                    .padding(space.med)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(space.large), contentAlignment = Alignment.Center) {
                    Text(title)
                }
                UseList(
                    checked = useList.value,
                    onCheckChanged = { useList.value = it },
                    modifier = Modifier.fillMaxWidth()
                )
                SelectCardType(
                    cardType = cardType.value,
                    onCardTypeSelected = {
                        cardType.value = it
                    },
                )
                GridSizeSelector(
                    Modifier.fillMaxWidth(),
                    onSizeSelected = {
                        gridCells.value = it
                    },
                    size = gridCells.value,
                )
                if (animatePlacement != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = animatePlacement.value,
                            onCheckedChange = { animatePlacement.value = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Animate item placement.",   style = MaterialTheme.typography.titleSmall,)
                    }
                }
                Spacer(
                    Modifier.windowInsetsPadding(WindowInsets.systemBars),
                )
            }
        }
    }
}

@Composable
fun GridSizeSelector(
    modifier: Modifier = Modifier,
    onSizeSelected: (Int) -> Unit,
    size: Int,
) {
    val space = LocalSpacing.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(
            Modifier.padding(horizontal = space.med),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Grid size",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$size per row",
                style =
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                ),
            )
        }
        Slider(
            modifier = Modifier.weight(1f),
            valueRange = 0f..100f,
            onValueChange = { value ->
                onSizeSelected(
                    when (value.roundToInt()) {
                        0 -> 1
                        in 0..25 -> 2
                        in 0..50 -> 3
                        in 0..75 -> 4
                        else -> 5
                    },
                )
            },
            steps = 3,
            value =
            when (size) {
                1 -> 0f
                2 -> 25f
                3 -> 50f
                4 -> 75f
                else -> 100f
            },
        )
        Text(
            text = "Reset",
            style =
            MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            ),
            modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .clickable { onSizeSelected(ExplorePrefs.gridCellsDefault) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    onBackClicked: () -> Unit,
    title: String
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}
@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val space = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(space.large)
        )
        Column(
            Modifier.padding(space.large)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
