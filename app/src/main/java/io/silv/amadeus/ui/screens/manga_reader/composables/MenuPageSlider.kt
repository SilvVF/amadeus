package io.silv.amadeus.ui.screens.manga_reader.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.silv.amadeus.ui.theme.LocalSpacing
import kotlin.math.roundToInt


@Composable
fun MenuPageSlider(
    visible: Boolean,
    page: Int,
    lastPage: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onPageChange: (page: Int) -> Unit
) {
    val space = LocalSpacing.current
    AnimatedVisibility(visible = visible) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(100))
            .background(Color.DarkGray.copy(alpha = 0.9f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevClick,
                modifier = Modifier.padding(space.small)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = null
                )
            }
            Text(
                text = page.toString(),
                modifier = Modifier.padding(space.small)
            )
            Slider(
                valueRange = 0f..lastPage.toFloat(),
                value = page.toFloat(),
                onValueChange = {
                    onPageChange(it.roundToInt())
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(space.small),
                steps = lastPage
            )
            Text(
                text = lastPage.toString(),
                modifier = Modifier.padding(space.small)
            )
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.padding(space.small)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = null
                )
            }
        }
    }
}