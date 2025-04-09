package io.silv.reader2

import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.silv.common.log.logcat
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.UUID

@Composable
fun TestPager(
    items: List<String>,
    pagerState: PagerState,
    scrollEnabled: Boolean,
) {
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        userScrollEnabled = scrollEnabled
    ) {
        Box(Modifier.fillMaxSize()) {
            Text("$it ${items.getOrNull(it)}", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview
@Composable
fun PreviewTestPagerHost() {
    MaterialTheme {
        TestPagerHost()
    }
}

fun getItem() = UUID.randomUUID().toString()

@Composable
fun TestPagerHost() {

    val items = remember { mutableStateListOf<String>() }
    var scrollEnabled by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(0) { items.size }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect {
                supervisorScope {
                    val page = pagerState.settledPage
                    try {
                        println("$page ${items.lastIndex - 1}")
                        if (page >= (items.lastIndex - 1)) {
                            pagerState.stopScroll()
                            scrollEnabled = false

                            val p = items.getOrNull(page)

                            val pi = items.toList()
                            items.clear()
                            items.addAll(
                                buildList {
                                    if (p != null) {
                                        pi.slice(pi.indexOf(p) - 1..pi.lastIndex).forEach {
                                            add(it)
                                        }
                                    }
                                    repeat(4) {
                                        add(getItem())
                                    }
                                }
                            )
                            println("pi $pi ->")
                            println("items ${items.toList()}")
                            if (p != null) {
                                pagerState.scrollToPage(items.indexOf(p))
                            }
                        }
                    } finally {
                        scrollEnabled = true
                    }
                }
            }
    }

    Surface(Modifier.fillMaxSize()) {
        TestPager(items, pagerState, scrollEnabled)
    }
}