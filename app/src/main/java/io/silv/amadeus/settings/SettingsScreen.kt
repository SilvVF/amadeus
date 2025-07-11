package io.silv.amadeus.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import io.silv.amadeus.settings.SettingsScreenModel.Dialog
import io.silv.common.model.AppTheme
import io.silv.common.model.AutomaticUpdatePeriod
import io.silv.common.model.CardType
import io.silv.datastore.Keys
import io.silv.explore.ExploreSettingsEvent
import io.silv.library.LibrarySettingsEvent
import io.silv.manga.filter.FilterSettingsEvent
import io.silv.reader.composables.ReaderOptions
import io.silv.ui.composables.SelectCardType
import io.silv.ui.composables.UseList
import io.silv.ui.theme.LocalSpacing
import kotlin.math.roundToInt

class SettingsScreen : Screen {

    @Composable
    override fun Content() {

        val screenModel = rememberScreenModel { SettingsScreenModel() }

        val state by screenModel.state.collectAsStateWithLifecycle()

        SettingsScreenContent(
            state = state,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreenContent(
    state: SettingsState,
) {

    val navigator = LocalNavigator.current
    val space = LocalSpacing.current

    val dismiss = { state.events(SettingsEvent.ChangeDialog(null)) }

    when (state.dialog) {
        Dialog.UpdatePeriod -> {
            BasicAlertDialog(
                onDismissRequest = dismiss,
                properties = DialogProperties(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shadowElevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.padding(space.xlarge)
                    ) {
                        Text("Automatic updates", style = MaterialTheme.typography.titleLarge)
                        AutomaticUpdatePeriod.entries.fastForEach {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.padding(space.med)
                            ) {
                                RadioButton(
                                    selected = state.appSettings.automaticUpdatePeriod == it,
                                    onClick = {
                                        state.appSettings.events(
                                            AppSettingsEvent.ChangeUpdateInterval(
                                                it
                                            )
                                        )
                                    }
                                )
                                Text(it.toString())
                            }
                        }
                    }
                }
            }
        }

        Dialog.Library -> {
            DisplayPrefsDialogContent(
                cardType = state.librarySettings.cardType,
                useList = state.librarySettings.useList,
                gridCells = state.librarySettings.gridCells,
                animatePlacement = state.librarySettings.animateItems,
                onDismiss = dismiss,
                changeUseList = { state.librarySettings.events(LibrarySettingsEvent.ToggleUseList) },
                changeAnimateItem = { state.librarySettings.events(LibrarySettingsEvent.ToggleAnimateItems) },
                changeCardType = {
                    state.librarySettings.events(
                        LibrarySettingsEvent.ChangeCardType(
                            it
                        )
                    )
                },
                changeGridCells = {
                    state.librarySettings.events(
                        LibrarySettingsEvent.ChangeGridCells(
                            it
                        )
                    )
                },
                title = "Library display options"
            )
        }

        Dialog.Explore -> {
            DisplayPrefsDialogContent(
                cardType = state.exploreSettings.cardType,
                useList = state.exploreSettings.useList,
                gridCells = state.exploreSettings.gridCells,
                onDismiss = dismiss,
                changeUseList = { state.exploreSettings.events(ExploreSettingsEvent.ToggleUseList) },
                changeCardType = {
                    state.exploreSettings.events(
                        ExploreSettingsEvent.ChangeCardType(
                            it
                        )
                    )
                },
                changeGridCells = {
                    state.exploreSettings.events(
                        ExploreSettingsEvent.ChangeGridCells(
                            it
                        )
                    )
                },
                title = "Explore display options"
            )
        }

        Dialog.Filter -> {
            DisplayPrefsDialogContent(
                cardType = state.filterSettings.cardType,
                useList = state.filterSettings.useList,
                gridCells = state.filterSettings.gridCells,
                onDismiss = dismiss,
                changeUseList = { state.filterSettings.events(FilterSettingsEvent.ToggleUseList) },
                changeCardType = { state.filterSettings.events(FilterSettingsEvent.ChangeCardType(it)) },
                changeGridCells = {
                    state.filterSettings.events(
                        FilterSettingsEvent.ChangeGridCells(
                            it
                        )
                    )
                },
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
                            selected = state.appSettings.theme == it,
                            onClick = { state.appSettings.events(AppSettingsEvent.ChangeTheme(it)) },
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
                    .clickable { state.events(SettingsEvent.ChangeDialog(Dialog.UpdatePeriod)) }
                    .padding(space.large),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text("Automatic updates", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(space.small))
                Text(
                    "${state.appSettings.automaticUpdatePeriod}",
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
                    icon = Icons.Filled.CollectionsBookmark
                ) {
                    state.events(SettingsEvent.ChangeDialog(Dialog.Library))
                }
                SettingsItem(
                    title = "Explore",
                    description = "explore display options",
                    icon = Icons.Filled.Explore
                ) {
                    state.events(SettingsEvent.ChangeDialog(Dialog.Explore))
                }
                SettingsItem(
                    title = "Filter",
                    description = "filter display options",
                    icon = Icons.Filled.AccessTime
                ) {
                    state.events(SettingsEvent.ChangeDialog(Dialog.Filter))
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
            ReaderOptions(settings = state.readerSettings)
        }
    }
}

@Composable
fun DisplayPrefsDialogContent(
    modifier: Modifier = Modifier,
    title: String,
    cardType: CardType,
    gridCells: Int,
    useList: Boolean,
    changeUseList: (Boolean) -> Unit,
    changeGridCells: (Int) -> Unit,
    changeCardType: (CardType) -> Unit,
    onDismiss: () -> Unit,
    changeAnimateItem: (Boolean) -> Unit = {},
    animatePlacement: Boolean? = null
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
                        .padding(space.large), contentAlignment = Alignment.Center
                ) {
                    Text(title)
                }
                UseList(
                    checked = useList,
                    onCheckChanged = { changeUseList(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                SelectCardType(
                    cardType = cardType,
                    onCardTypeSelected = {
                        changeCardType(it)
                    },
                )
                GridSizeSelector(
                    Modifier.fillMaxWidth(),
                    onSizeSelected = changeGridCells,
                    size = gridCells,
                )
                if (animatePlacement != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = animatePlacement,
                            onCheckedChange = changeAnimateItem
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Animate item placement.",
                            style = MaterialTheme.typography.titleSmall,
                        )
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
                    .clickable { onSizeSelected(Keys.GRID_CELLS_DEFAULT) },
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
