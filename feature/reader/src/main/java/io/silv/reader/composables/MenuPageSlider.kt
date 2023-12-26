package io.silv.reader.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.dp
import io.silv.ui.theme.LocalSpacing
import kotlin.math.roundToInt

@Composable
fun MenuPageSlider(
    modifier: Modifier = Modifier,
    fraction: Float,
    layoutDirection: LayoutDirection,
    pageIdx: Int,
    pageCount: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onPageChange: (page: Int) -> Unit,
) {
    val space = LocalSpacing.current
    
    fun leftButtonClick() {
        when (layoutDirection) {
            Ltr -> onPrevClick()
            Rtl -> onNextClick()
        }
    }
    
    fun rightButtonClick() {
        when (layoutDirection) {
            Ltr -> onNextClick()
            Rtl -> onPrevClick()
        }
    }
    val elevatedSurfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(CircleShape)
            .drawBehind {
                drawRect(
                    color = elevatedSurfaceColor.copy(alpha = fraction),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = ::leftButtonClick,
            modifier = Modifier.padding(space.small),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = LocalContentColor.current.copy(alpha = fraction)
            )
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = null
            )
        }
        Text(
            text = when (layoutDirection) {
                Ltr -> "${pageIdx + 1}"
                Rtl -> pageCount.toString()
            },
            color = MaterialTheme.colorScheme.primary.copy(alpha = fraction),
            modifier = Modifier.padding(space.small)
        )
        if (fraction > 0f) {
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection
            ) {
                Slider(
                    valueRange = 1f..pageCount.toFloat().coerceAtLeast(1f),
                    value = (pageIdx + 1f),
                    onValueChange = { onPageChange((it - 1f).roundToInt()) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(space.small),
                    steps = (pageCount - 2).coerceAtLeast(0),
                    colors = sliderColors(fraction = fraction)
                )
            }
        }
        Text(
            text = when (layoutDirection) {
                Rtl -> "${pageIdx + 1}"
                Ltr -> pageCount.toString()
            },
            modifier = Modifier.padding(space.small),
            color = MaterialTheme.colorScheme.primary.copy(alpha = fraction),
        )
        IconButton(
            onClick = ::rightButtonClick,
            modifier = Modifier.padding(space.small),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = LocalContentColor.current.copy(alpha = fraction)
            )
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun sliderColors(fraction: Float): SliderColors = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = fraction),
    disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = fraction).compositeOver(
        MaterialTheme.colorScheme.surface),
    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = fraction),
    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = fraction),
    disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = fraction),
    disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = fraction),
    activeTickColor = MaterialTheme.colorScheme.contentColorFor(
        MaterialTheme.colorScheme.primary.copy(alpha = fraction)
    ).copy(alpha = fraction),
    inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = fraction),
)
