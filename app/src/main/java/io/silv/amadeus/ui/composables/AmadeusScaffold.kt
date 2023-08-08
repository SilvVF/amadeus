package io.silv.amadeus.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import io.silv.amadeus.AmadeusBottomBar
import io.silv.amadeus.AmadeusNavRail
import io.silv.amadeus.LocalWindowSizeClass
import io.silv.amadeus.ui.screens.home.shouldShowBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmadeusScaffold(
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState()),
    showBottomBar: Boolean = true,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current
    if (shouldShowBottomBar(windowSizeClass)) {
        Scaffold(
            topBar = {
                topBar()
            },
            bottomBar = {
                AmadeusBottomBar()
            },
            snackbarHost = snackbarHost,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            content(it)
        }
    } else {
        Row {
            AmadeusNavRail(visible = showBottomBar)
            Scaffold(
                topBar = {
                    topBar()
                },
                snackbarHost = snackbarHost,
                contentColor = contentColor,
                contentWindowInsets = contentWindowInsets,
                modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                content(it)
            }
        }
    }
}
