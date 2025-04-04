package io.silv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleStartEffect
import cafe.adriel.voyager.navigator.tab.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface ReselectTab : Tab {
    val reselectCh: Channel<Unit>
}

@Composable
fun ReselectTab.LaunchedOnReselect(onReselect: suspend () -> Unit) {

    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onReselect)

    LifecycleStartEffect(Unit) {
        val job = scope.launch(Dispatchers.Main.immediate) {
            for (select in reselectCh) {
                callback()
            }
        }
        onStopOrDispose { job.cancel() }
    }
}

interface GlobalSearchTab : Tab {
    val searchCh: Channel<String>
}

@Composable
fun GlobalSearchTab.LaunchedOnSearch(onSearch: suspend (String) -> Unit) {

    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onSearch)

    LifecycleStartEffect(Unit) {
        val job = scope.launch(Dispatchers.Main.immediate)  {
            for (query in searchCh) {
                callback(query)
            }
        }
        onStopOrDispose { job.cancel() }
    }
}