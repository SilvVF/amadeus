package io.silv.ui

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize

fun LazyGridScope.header(
    key: Any? = null,
    content: @Composable LazyGridItemScope.() -> Unit,
) {
    item(
        key = key,
        span = { GridItemSpan(this.maxLineSpan) },
        content = content,
    )
}

fun Modifier.vertical() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2),
            )
        }
    }

fun Modifier.getSize(onChange: (DpSize) -> Unit) =
    composed {
        val density = LocalDensity.current
        onGloballyPositioned {
            with(density) {
                onChange(
                    DpSize(
                        width = it.size.width.toDp(),
                        height = it.size.height.toDp(),
                    ),
                )
            }
        }
    }
