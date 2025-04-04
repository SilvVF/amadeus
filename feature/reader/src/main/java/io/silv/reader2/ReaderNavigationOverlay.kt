package io.silv.reader2

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import io.silv.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Preview()
@Composable
fun PreviewReaderNavigationOverlay() {
    MaterialTheme {
        ReaderNavigationOverlay(
            modifier = Modifier.fillMaxSize(),
            state = rememberReaderOverlayState()
        ) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
        }
    }
}

private const val ANIM_DURATION = 1000

@Composable
fun rememberReaderOverlayState(
    navigation: ViewerNavigation = remember { RightAndLeftNavigation() },
    spec:  AnimationSpec<Float> = tween(durationMillis = ANIM_DURATION),
    scope: CoroutineScope = rememberCoroutineScope()
): ReaderOverlayState {

    var firstLaunch by rememberSaveable { mutableStateOf(true) }

    return remember(navigation, spec, scope) {
        ReaderOverlayState(
            navigation,
            firstLaunch.also { firstLaunch = false },
            spec,
            scope
        )
    }
}

class ReaderOverlayState(
    val navigation: ViewerNavigation,
    firstLaunch: Boolean,
    private val spec: AnimationSpec<Float>,
    private val scope: CoroutineScope
) {

    val alphaAnim = Animatable(0f)

    val regionPaint = Paint()

    val textPaint =
        Paint().apply {
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
            textSize = 64f
        }

    val textBorderPaint =
        Paint().apply {
            textAlign = Paint.Align.CENTER
            color = Color.BLACK
            textSize = 64f
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

    init {
        if (firstLaunch) {
            scope.launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    spec
                )
            }
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            scope.launch {
                alphaAnim.animateTo(
                    targetValue = 0f,
                    spec
                )
            }
            true
        } else {
            false
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ReaderNavigationOverlay(
    modifier: Modifier = Modifier,
    state: ReaderOverlayState,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier
            .pointerInteropFilter { event ->
                state.onTouchEvent(event)
            }
            .graphicsLayer {
                alpha = state.alphaAnim.value
            }
            .drawWithContent {
                drawContent()
                val canvas = drawContext.canvas.nativeCanvas
                state.navigation.getRegions().forEach { region ->
                    val rect = region.rectF

                    // Scale rect from 1f,1f to screen width and height
                    canvas.withScale(size.width.toFloat(), size.height.toFloat()) {
                        state.regionPaint.color = region.type.color
                        drawRect(rect, state.regionPaint)
                    }

                    // Don't want scale anymore because it messes with drawText
                    // Translate origin to rect start (left, top)
                    canvas.withTranslation(
                        x = (size.width * rect.left),
                        y = (size.height * rect.top)
                    ) {
                        // Calculate center of rect width on screen
                        val x = width * (abs(rect.left - rect.right) / 2)

                        // Calculate center of rect height on screen
                        val y = height * (abs(rect.top - rect.bottom) / 2)

                        drawText(context.getString(region.type.nameRes), x, y, state.textBorderPaint)
                        drawText(context.getString(region.type.nameRes), x, y, state.textPaint)
                    }
                }
            }
    ) {
        content()
    }
}

abstract class ViewerNavigation {

    sealed class NavigationRegion(@StringRes val nameRes: Int, val color: Int) {
        data object MENU : NavigationRegion(
            R.string.action_menu,
            Color.argb(0xCC, 0x95, 0x81, 0x8D),
        )

        data object PREV : NavigationRegion(
            R.string.nav_zone_prev,
            Color.argb(0xCC, 0xFF, 0x77, 0x33),
        )

        data object NEXT : NavigationRegion(
            R.string.nav_zone_next,
            Color.argb(0xCC, 0x84, 0xE2, 0x96),
        )

        data object LEFT : NavigationRegion(
            R.string.nav_zone_left,
            Color.argb(0xCC, 0x7D, 0x11, 0x28),
        )

        data object RIGHT : NavigationRegion(
            R.string.nav_zone_right,
            Color.argb(0xCC, 0xA6, 0xCF, 0xD5),
        )
    }

    data class Region(
        val rectF: RectF,
        val type: NavigationRegion,
    )

    private var constantMenuRegion: RectF = RectF(0f, 0f, 1f, 0.05f)

    protected abstract var regionList: List<Region>

    /** Returns regions with applied inversion. */
    fun getRegions(): List<Region> {
        return regionList
    }

    fun getAction(pos: PointF): NavigationRegion {
        val x = pos.x
        val y = pos.y
        val region = getRegions().find { it.rectF.contains(x, y) }
        return when {
            region != null -> region.type
            constantMenuRegion.contains(x, y) -> NavigationRegion.MENU
            else -> NavigationRegion.MENU
        }
    }
}


/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | N | M | P |   P: Move Right
 * +---+---+---+
 * | N | M | P |   M: Menu
 * +---+---+---+
 * | N | M | P |   N: Move Left
 * +---+---+---+
 */
class RightAndLeftNavigation : ViewerNavigation() {

    override var regionList: List<Region> = listOf(
        Region(
            rectF = RectF(0f, 0f, 0.33f, 1f),
            type = NavigationRegion.LEFT,
        ),
        Region(
            rectF = RectF(0.66f, 0f, 1f, 1f),
            type = NavigationRegion.RIGHT,
        ),
    )
}

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | P | P | P |   P: Previous
 * +---+---+---+
 * | P | M | N |   M: Menu
 * +---+---+---+
 * | N | N | N |   N: Next
 * +---+---+---+
 */
open class LNavigation : ViewerNavigation() {

    override var regionList: List<Region> = listOf(
        Region(
            rectF = RectF(0f, 0.33f, 0.33f, 0.66f),
            type = NavigationRegion.PREV,
        ),
        Region(
            rectF = RectF(0f, 0f, 1f, 0.33f),
            type = NavigationRegion.PREV,
        ),
        Region(
            rectF = RectF(0.66f, 0.33f, 1f, 0.66f),
            type = NavigationRegion.NEXT,
        ),
        Region(
            rectF = RectF(0f, 0.66f, 1f, 1f),
            type = NavigationRegion.NEXT,
        ),
    )
}