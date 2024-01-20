package io.silv.reader.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.silv.ui.CenterBox
import kotlinx.coroutines.delay

@Composable
fun GestureHintOverlay(
    modifier: Modifier = Modifier,
    duration: Long = 2000,
    vertical: Boolean = false,
    content: @Composable () -> Unit
) {

    val layoutDirection = LocalLayoutDirection.current

    var shownMenuHint by rememberSaveable { mutableStateOf<LayoutDirection?>(null) }
    var menuHintVisible by remember { mutableStateOf(false) }

    LaunchedEffect(layoutDirection) {
        if (shownMenuHint != layoutDirection) {
            shownMenuHint = layoutDirection
            menuHintVisible = true
            delay(duration)
            menuHintVisible = false
        }
    }


    Box(modifier) {

        content()

        if (menuHintVisible) {
            if (vertical) {
                VerticalMenuHint { menuHintVisible = false }
            } else {
                MenuHint { menuHintVisible = false }
            }
        }
    }
}

@Composable
private fun VerticalMenuHint(
    hide: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .clickable { hide() }) {
        val ld = LocalLayoutDirection.current
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Green.copy(alpha = 0.38f))) {
            Text("Prev")
        }
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.38f))) {
            Text("Menu")
        }
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Blue.copy(alpha = 0.38f))) {
            Text("Next")
        }
    }
}

@Composable
private fun MenuHint(
    hide: () -> Unit
) {
    Row(
        Modifier
            .fillMaxSize()
            .clickable { hide() }) {
        val ld = LocalLayoutDirection.current
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Green.copy(alpha = 0.38f))) {
            Text(
                when(ld) {
                    LayoutDirection.Ltr -> "Next"
                    LayoutDirection.Rtl -> "Prev"
                }
            )
        }
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.38f))) {
            Text("Menu")
        }
        CenterBox(modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .background(Color.Blue.copy(alpha = 0.38f))) {
            Text(
                when(ld) {
                    LayoutDirection.Ltr -> "Prev"
                    LayoutDirection.Rtl -> "Next"
                }
            )
        }
    }
}