package io.silv.manga.filter

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import io.silv.common.model.CardType
import io.silv.datastore.Keys
import io.silv.datastore.SettingsStore
import io.silv.ui.composables.SelectCardType
import io.silv.ui.composables.UseList
import io.silv.ui.theme.LocalSpacing
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.datastore.preferences.core.Preferences
import app.cash.molecule.AndroidUiDispatcher
import kotlinx.coroutines.launch

sealed interface FilterSettingsEvent {
    data object ToggleUseList: FilterSettingsEvent
    data class ChangeCardType(val cardType: CardType): FilterSettingsEvent
    data class ChangeGridCells(val gridCells: Int): FilterSettingsEvent
}

data class FilterSettings(
    val useList: Boolean = false,
    val cardType: CardType = CardType.Compact,
    val gridCells: Int = Keys.GRID_CELLS_DEFAULT,
    val events: (FilterSettingsEvent) -> Unit = {}
)

class FilterSettingsPresenter(
    parentScope: CoroutineScope,
    private val store: SettingsStore,
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + AndroidUiDispatcher.Main)

    val state = scope.launchMolecule(RecompositionMode.ContextClock) {
        present()
    }

    @Composable
    fun present(): FilterSettings {

        val scope = rememberCoroutineScope()

        val useList by rememberUpdatedState(store.filterUseList.collectAsState().value)
        val cardType by store.filterCardType.collectAsState()
        val gridCells by store.filterGridCells.collectAsState()

        fun <T> editSettings(key: Preferences.Key<T>, value: T) {
            scope.launch {
                store.edit { prefs ->
                    prefs[key] = value
                }
            }
        }

        return FilterSettings(
            useList = useList,
            cardType = cardType,
            gridCells = gridCells,
        ) { event ->
            when(event) {
                is FilterSettingsEvent.ChangeCardType -> editSettings(Keys.FilterPrefs.cardTypePrefKey, event.cardType.ordinal)
                is FilterSettingsEvent.ChangeGridCells -> editSettings(Keys.FilterPrefs.gridCellsPrefKey, event.gridCells)
                FilterSettingsEvent.ToggleUseList -> editSettings(Keys.FilterPrefs.useListPrefKey, !useList)
            }
        }
    }

}

@Composable
fun FilterDisplayOptionsBottomSheet(
    settings: FilterSettings,
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
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                optionsTitle()
            }
            UseList(
                checked = settings.useList,
                onCheckChanged = { settings.events(FilterSettingsEvent.ToggleUseList) },
                modifier = Modifier.fillMaxWidth()
            )
            SelectCardType(
                cardType = settings.cardType,
                onCardTypeSelected = { settings.events(FilterSettingsEvent.ChangeCardType(it)) },
            )
            GridSizeSelector(
                Modifier.fillMaxWidth(),
                onSizeSelected = {
                    settings.events(FilterSettingsEvent.ChangeGridCells(it))
                },
                size = settings.gridCells,
            )
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars))
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
