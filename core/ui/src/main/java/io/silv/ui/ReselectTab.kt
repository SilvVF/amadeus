package io.silv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.navigator.tab.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ReselectTab : Tab {
    val reselectCh: Channel<Unit>
}

@Composable
fun ReselectTab.LaunchedOnReselect(onReselect: suspend () -> Unit) {

    val callback by rememberUpdatedState(onReselect)

    reselectCh.collectEvents { callback() }
}

interface GlobalSearchTab : Tab {
    val searchCh: Channel<String>
}

@Composable
fun GlobalSearchTab.LaunchedOnSearch(onSearch: suspend (String) -> Unit) {
    val callback by rememberUpdatedState(onSearch)

    searchCh.collectEvents { callback(it) }
}