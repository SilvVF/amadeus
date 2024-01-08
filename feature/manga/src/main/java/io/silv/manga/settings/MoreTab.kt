package io.silv.manga.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.silv.ui.CenterBox
import io.silv.ui.ReselectTab

object MoreTab: ReselectTab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = "More",
            icon = rememberVectorPainter(image = Icons.Filled.MoreHoriz)
        )

    @Composable
    override fun Content() {
        CenterBox(Modifier.fillMaxSize()) {
            Text("More")
        }
    }
}