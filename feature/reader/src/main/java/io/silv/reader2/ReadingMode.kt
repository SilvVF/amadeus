package io.silv.reader2

import android.content.pm.ActivityInfo
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.ui.graphics.vector.ImageVector
import io.silv.ui.R

enum class Reader2Orientation(
    val flag: Int,
    @StringRes val stringRes: Int,
    val icon: ImageVector,
    val flagValue: Int,
) {
    DEFAULT(
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        R.string.label_default,
        Icons.Default.ScreenRotation,
        0x00000000,
    ),
    FREE(
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        R.string.rotation_free,
        Icons.Default.ScreenRotation,
        0x00000008,
    ),
    PORTRAIT(
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
        R.string.rotation_portrait,
        Icons.Default.StayCurrentPortrait,
        0x00000010,
    ),
    LANDSCAPE(
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        R.string.rotation_landscape,
        Icons.Default.StayCurrentLandscape,
        0x00000018,
    ),
    LOCKED_PORTRAIT(
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        R.string.rotation_force_portrait,
        Icons.Default.ScreenLockPortrait,
        0x00000020,
    ),
    LOCKED_LANDSCAPE(
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        R.string.rotation_force_landscape,
        Icons.Default.ScreenLockLandscape,
        0x00000028,
    ),
    REVERSE_PORTRAIT(
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        R.string.rotation_reverse_portrait,
        Icons.Default.StayCurrentPortrait,
        0x00000030,
    ),
    ;

    companion object {
        const val MASK = 0x00000038

        fun fromPreference(preference: Int?): Reader2Orientation = entries.find { it.flagValue == preference } ?: DEFAULT
    }
}

enum class ReadingMode(
    @StringRes val stringRes: Int,
    @DrawableRes val iconRes: Int,
    val flagValue: Int,
    val direction: Direction? = null,
    val type: ViewerType? = null,
) {
    DEFAULT(R.string.label_default, io.silv.data.R.drawable.amadeuslogo, 0x00000000),
    LEFT_TO_RIGHT(
        R.string.left_to_right_viewer,
        io.silv.data.R.drawable.amadeuslogo,
        0x00000001,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    RIGHT_TO_LEFT(
        R.string.right_to_left_viewer,
        io.silv.data.R.drawable.amadeuslogo,
        0x00000002,
        Direction.Horizontal,
        ViewerType.Pager,
    ),
    VERTICAL(
        R.string.vertical_viewer,
        io.silv.data.R.drawable.amadeuslogo,
        0x00000003,
        Direction.Vertical,
        ViewerType.Pager,
    ),
    WEBTOON(
        R.string.webtoon_viewer,
        io.silv.data.R.drawable.amadeuslogo,
        0x00000004,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    CONTINUOUS_VERTICAL(
        R.string.vertical_plus_viewer,
        io.silv.data.R.drawable.amadeuslogo,
        0x00000005,
        Direction.Vertical,
        ViewerType.Webtoon,
    ),
    ;

    companion object {
        const val MASK = 0x00000007

        fun fromPreference(preference: Int?): ReadingMode = entries.find { it.flagValue == preference } ?: DEFAULT

        fun isPagerType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode.type is ViewerType.Pager
        }

        fun toViewer(preference: Int?): Viewer {
            return when (fromPreference(preference)) {
                LEFT_TO_RIGHT -> TODO()
                ReadingMode.DEFAULT -> TODO()
                ReadingMode.RIGHT_TO_LEFT -> TODO()
                ReadingMode.VERTICAL -> TODO()
                ReadingMode.WEBTOON -> TODO()
                ReadingMode.CONTINUOUS_VERTICAL -> TODO()
            }
        }
    }

    sealed interface Direction {
        data object Horizontal : Direction
        data object Vertical : Direction
    }

    sealed interface ViewerType {
        data object Pager : ViewerType
        data object Webtoon : ViewerType
    }
}
