package io.silv.reader.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.skydoves.orbital.Orbital
import com.skydoves.orbital.animateMovement
import com.skydoves.orbital.rememberContentWithOrbitalScope

@Composable
fun <T> OrbitalSwitch(
    modifier: Modifier = Modifier,
    title: String,
    labelLeft: String,
    labelRight: String,
    itemLeft: T,
    itemRight: T,
    mode: T,
    icon: (@Composable () -> Unit)? = null,
    onModeChange: (T) -> Unit,
) {
    val space = io.silv.ui.theme.LocalSpacing.current
    val transformationSpec = SpringSpec<IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = 4000f
    )
    val transformed = remember(mode) { mode != itemLeft}

    val switch = rememberContentWithOrbitalScope {
        io.silv.ui.CenterBox(
            Modifier.animateMovement(
                this@rememberContentWithOrbitalScope,
                transformationSpec
            )
        ) {
            io.silv.ui.CenterBox(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                icon?.invoke()
            }
        }
    }
    Column(modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = labelLeft,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (transformed) {
                        Color.Unspecified
                    } else  MaterialTheme.colorScheme.primary,
                    fontWeight = if (transformed) {
                        FontWeight.Normal
                    } else  FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = space.med)
            )
            Orbital(
                Modifier
                    .width(60.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(100))
                    .clickable {
                        onModeChange(mode)
                    }
            ) {
                if (!transformed) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(100))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                3.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(100)
                            )
                            .padding(space.small),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        switch()
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(100))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                3.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(100)
                            )
                            .padding(space.small),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        switch()
                    }
                }
            }
            Text(
                text = labelRight,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (!transformed) {
                        Color.Unspecified
                    } else  MaterialTheme.colorScheme.primary,
                    fontWeight = if (!transformed) {
                        FontWeight.Normal
                    } else  FontWeight.Bold
                ),
                modifier =  Modifier.padding(horizontal = space.med)
            )
        }
    }
}