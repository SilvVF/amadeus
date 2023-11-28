package io.silv.amadeus.ui.screens.manga_reader.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.silv.amadeus.ui.theme.LocalSpacing
import io.silv.common.model.ReaderDirection
import io.silv.common.model.ReaderOrientation
import io.silv.datastore.model.ReaderSettings

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
            itemLeft = ReaderDirection.Ltr,
            itemRight = ReaderDirection.Rtl
        ) {
            onSettingsChanged(
                settings.copy(
                    direction = if (it == ReaderDirection.Ltr)
                        ReaderDirection.Rtl
                    else
                        ReaderDirection.Ltr
                )
            )
        }
        Spacer(modifier = Modifier.height(space.large))
        OrbitalSwitch<ReaderOrientation>(
            title = "reader orientation",
            mode = settings.orientation,
            labelLeft = "vertical",
            labelRight = "horizontal",
            itemLeft = ReaderOrientation.Vertical,
            itemRight = ReaderOrientation.Horizontal
        ) { enum ->
            onSettingsChanged(
                settings.copy(
                    orientation = if (enum == ReaderOrientation.Vertical) {
                        ReaderOrientation.Horizontal
                    } else {
                        ReaderOrientation.Vertical
                    }
                )
            )
        }
    }
}