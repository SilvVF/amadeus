package io.silv.explore.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import io.silv.explore.UiQueryFilters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersBottomSheet(
    onSaveQuery: (UiQueryFilters) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        FilterBottomSheetContent(
            hide = onDismissRequest,
            onSaveQueryClick = {
                scope.launch {
                    sheetState.hide()
                    onDismissRequest()
                }
                onSaveQuery(it)
            },
            onQueryFilterChange = {},
        )
    }
}
