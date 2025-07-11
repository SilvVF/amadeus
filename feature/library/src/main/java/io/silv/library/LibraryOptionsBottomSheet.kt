package io.silv.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import io.silv.common.model.CardType
import io.silv.datastore.Keys
import io.silv.datastore.SettingsStore
import io.silv.ui.composables.SelectCardType
import io.silv.ui.composables.UseList
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface LibrarySettingsEvent {
    data class ChangeCardType(val cardType: CardType): LibrarySettingsEvent
    data class ChangeGridCells(val gridCells: Int): LibrarySettingsEvent
    data object ToggleUseList: LibrarySettingsEvent
    data object ToggleAnimateItems: LibrarySettingsEvent
}

data class LibrarySettings(
    val cardType: CardType = CardType.Compact,
    val gridCells: Int = 2,
    val useList: Boolean = false,
    val animateItems: Boolean = true,
    val events: (LibrarySettingsEvent) -> Unit = {}
)

class LibrarySettingsPresenter(
    parentScope: CoroutineScope,
    private val settingsStore: SettingsStore
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + AndroidUiDispatcher.Main)

    val state = scope.launchMolecule(mode = RecompositionMode.ContextClock) {
        present()
    }

    @Composable
    fun present(): LibrarySettings {
        val scope = rememberCoroutineScope()

        val cardType by settingsStore.libraryCardType.collectAsState()
        val cells by settingsStore.libraryGridCells.collectAsState()
        val useList by rememberUpdatedState(settingsStore.libraryUseList.collectAsState().value)
        val animateItems by rememberUpdatedState(settingsStore.libraryAnimatePlacementPrefKey.collectAsState().value)

        fun <T> editSettings(
            key: Preferences.Key<T>,
            value: T
        ) = scope.launch {
            settingsStore.edit { prefs ->
                prefs[key] = value
            }
        }

        return LibrarySettings(
            cardType = cardType,
            gridCells = cells,
            useList = useList,
            animateItems = animateItems
        ) {
            when(it) {
                is LibrarySettingsEvent.ChangeCardType -> editSettings(Keys.LibraryPrefs.cardTypePrefKey, it.cardType.ordinal)
                is LibrarySettingsEvent.ChangeGridCells -> editSettings(Keys.LibraryPrefs.gridCellsPrefKey, it.gridCells)
                LibrarySettingsEvent.ToggleUseList -> editSettings(Keys.LibraryPrefs.useListPrefKey, !useList)
                LibrarySettingsEvent.ToggleAnimateItems -> editSettings(Keys.LibraryPrefs.animatePlacementPrefKey, !animateItems)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryOptionsBottomSheet(
    state: LibrarySettings,
    optionsTitle: @Composable () -> Unit = {},
    onDismissRequest: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )


    LaunchedEffect(Unit) {
        sheetState.show()
    }


    ModalBottomSheet(
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                optionsTitle()
            }
            UseList(
                checked = state.useList,
                onCheckChanged = { state.events(LibrarySettingsEvent.ToggleUseList) },
                modifier = Modifier.fillMaxWidth()
            )
            SelectCardType(
                cardType = state.cardType,
                onCardTypeSelected = {
                    state.events(LibrarySettingsEvent.ChangeCardType(it))
                },
            )
            GridSizeSelector(
                Modifier.fillMaxWidth(),
                onSizeSelected = {
                    state.events(LibrarySettingsEvent.ChangeGridCells(it))
                },
                size = state.gridCells,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.animateItems,
                    onCheckedChange = { state.events(LibrarySettingsEvent.ToggleAnimateItems) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Animate item placement.", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(
                Modifier.windowInsetsPadding(WindowInsets.systemBars),
            )
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