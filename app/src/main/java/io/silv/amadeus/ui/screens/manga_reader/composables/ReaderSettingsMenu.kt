package io.silv.amadeus.ui.screens.manga_reader.composables

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import io.silv.amadeus.data.ReaderSettings
import io.silv.amadeus.ui.theme.LocalSpacing

@Composable
fun ReaderSettingsMenu(
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit
) {
    val space = LocalSpacing.current
    Column {
        OrbitalSwitch(
            title = "reader direction",
            mode = settings.direction,
            labelLeft = "Ltr",
            labelRight = "Rtl",
            itemLeft = LayoutDirection.Ltr,
            itemRight = LayoutDirection.Rtl
        ) {
            onSettingsChanged(
                settings.copy(
                    direction =if (it == LayoutDirection.Ltr)
                        LayoutDirection.Rtl
                    else
                        LayoutDirection.Ltr
                )
            )
        }
        Spacer(modifier = Modifier.height(space.large))
        OrbitalSwitch(
            title = "reader orientation",
            mode = settings.orientation,
            labelLeft = "vertical",
            labelRight = "horizontal",
            itemLeft = Orientation.Vertical,
            itemRight = Orientation.Horizontal
        ) {
            onSettingsChanged(
                settings.copy(
                    orientation = if (it == Orientation.Vertical)
                        Orientation.Horizontal
                    else
                        Orientation.Vertical
                )
            )
        }
    }
}