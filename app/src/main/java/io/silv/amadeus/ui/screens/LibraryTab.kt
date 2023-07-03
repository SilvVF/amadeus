package io.silv.amadeus.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.amadeus.ui.shared.CenterBox
import io.silv.amadeus.ui.theme.LocalBottomBarVisibility

object LibraryTab: Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "library"
            val icon = rememberVectorPainter(Icons.Default.LibraryBooks)

            return remember {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        CenterBox(Modifier.fillMaxSize()) {

            var bottomBarVisiblity by LocalBottomBarVisibility.current

            Text("library")
            Button(onClick = { bottomBarVisiblity = !bottomBarVisiblity}) {

            }
        }
    }

}