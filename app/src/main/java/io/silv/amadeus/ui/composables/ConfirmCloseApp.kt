package io.silv.amadeus.ui.composables

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.silv.amadeus.ui.shared.noRippleClickable
import io.silv.amadeus.ui.theme.LocalSpacing

@Composable
fun ConfirmCloseAppPopup() {

    val space = LocalSpacing.current

    var closeAppPopupVisible by remember { mutableStateOf(false) }

    BackHandler(
        enabled = !closeAppPopupVisible
    ) {
        closeAppPopupVisible = true
    }

    val backHandler = LocalOnBackPressedDispatcherOwner.current

    if (closeAppPopupVisible) {
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = { closeAppPopupVisible = false },
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .noRippleClickable { closeAppPopupVisible = false  }
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(space.large)
            ) {
                Text(
                    text = "click the button to close the app or tap anywhere on the screen to resume"
                )
                Button(
                    onClick = {
                        backHandler?.onBackPressedDispatcher?.onBackPressed()
                    }
                ) {
                    Text(text = "Close app")
                }
            }
        }
    }
}