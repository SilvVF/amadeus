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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    fractionProvider:() -> Float,
    layoutDirectionProvider: () -> LayoutDirection,
    currentPageProvider: () -> Int,
    pageCountProvider: () -> Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onPageChange: (page: Int) -> Unit,
) {
    val pageCount = pageCountProvider()
    val currentPage = currentPageProvider()

    val valid = !(currentPage <= 0 || pageCount <= 0)

    val sliderState = remember(currentPage) {
        SliderState(
            value = currentPage.toFloat().coerceAtLeast(1f),
            steps = pageCount.coerceAtLeast(1),
            valueRange = 1f..pageCount.toFloat()
        )
    }

    SideEffect {
        sliderState.onValueChangeFinished = {
            onPageChange(sliderState.value.roundToInt())
        }
    }

    val space = LocalSpacing.current
    val layoutDirection = layoutDirectionProvider()

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
    val fraction = fractionProvider()

    Row(
        modifier = modifier
            .padding(bottom = space.large)
            .fillMaxWidth()
            .height(60.dp)
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
            text = if (valid) "${sliderState.value.toInt()}" else "",
            color = MaterialTheme.colorScheme.primary.copy(alpha = fraction),
            modifier = Modifier.padding(space.small)
        )
        if (fraction > 0f && valid) {
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection
            ) {


                Slider(
                    state = sliderState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(space.small),
                    colors = sliderColors(fraction = fraction),

                )
            }
        }
        Text(
            text = if (valid) "$pageCount" else "",
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
