package io.silv.amadeus.ui.shared

import android.graphics.BlurMaskFilter
import androidx.annotation.FloatRange
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

fun Modifier.shadow(
    color: Color = Color.Black,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    blurRadius: Dp = 0.dp,
) = then(
    drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            if (blurRadius != 0.dp) {
                frameworkPaint.maskFilter = (BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL))
            }
            frameworkPaint.color = color.toArgb()

            val leftPixel = offsetX.toPx()
            val topPixel = offsetY.toPx()
            val rightPixel = size.width + topPixel
            val bottomPixel = size.height + leftPixel

            canvas.drawRect(
                left = leftPixel,
                top = topPixel,
                right = rightPixel,
                bottom = bottomPixel,
                paint = paint,
            )
        }
    }
)

fun Modifier.fillMaxAfterMesaure(context: BoxScope, @FloatRange(0.0, 1.0, true, true) size: Float) = with(context) {
    this@fillMaxAfterMesaure
        .matchParentSize()
        .layout { measurable, constraints ->
            // Measure the composable
            val placeable = measurable.measure(constraints)

            layout(placeable.width, placeable.height) {
                placeable.place(0, -(constraints.maxHeight * (1f - size)).roundToInt())
            }
        }
}



object StringStateListSaver: Saver<SnapshotStateList<String>, String> {
    override fun restore(value: String): SnapshotStateList<String> {
        return mutableStateListOf<String>().also { list ->
            list.addAll(
                value.split(",")
            )
        }
    }

    override fun SaverScope.save(value: SnapshotStateList<String>): String {
        return value.joinToString()
    }
}

inline fun Modifier.noRippleClickable(
    crossinline onClick: () -> Unit
): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

@Composable
fun CenterBox(
    modifier: Modifier = Modifier,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        propagateMinConstraints = propagateMinConstraints,
        content = content
    )
}
