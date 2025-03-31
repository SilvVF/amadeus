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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.silv.datastore.ExplorePrefs
import io.silv.datastore.LibraryPrefs
import io.silv.datastore.collectAsState
import io.silv.ui.Converters
import io.silv.ui.composables.CardType
import io.silv.ui.composables.SelectCardType
import io.silv.ui.composables.UseList
import io.silv.ui.theme.LocalSpacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryOptionsBottomSheet(
    optionsTitle: @Composable () -> Unit = {},
    onDismissRequest: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    val scope = rememberCoroutineScope()

    var cardType by LibraryPrefs.cardTypePrefKey.collectAsState(
        defaultValue = CardType.Compact,
        converter = Converters.CardTypeToStringConverter,
        scope = scope,
    )

    var gridCells by LibraryPrefs.gridCellsPrefKey.collectAsState(LibraryPrefs.gridCellsDefault, scope)
    var useList by LibraryPrefs.useListPrefKey.collectAsState(false, scope)
    var animatePlacement by LibraryPrefs.animatePlacementPrefKey.collectAsState(true, scope)


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
                checked = useList,
                onCheckChanged = { useList = it },
                modifier = Modifier.fillMaxWidth()
            )
            SelectCardType(
                cardType = cardType,
                onCardTypeSelected = {
                    cardType = it
                },
            )
            GridSizeSelector(
                Modifier.fillMaxWidth(),
                onSizeSelected = {
                    gridCells = it
                },
                size = gridCells,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = animatePlacement,
                    onCheckedChange = { animatePlacement = it }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Animate item placement.",   style = MaterialTheme.typography.titleSmall,)
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
                .clickable { onSizeSelected(ExplorePrefs.gridCellsDefault) },
        )
    }
}