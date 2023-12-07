package io.silv.explore

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.ui.ReselectTab
import kotlinx.coroutines.channels.Channel

object ExploreTab: ReselectTab {

    internal val reselectChannel = Channel<Unit>()

    override suspend fun onReselect(navigator: Navigator) {
        Log.d("Explore", "Sending reselect event")
        reselectChannel.send(Unit)
    }

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 0u,
                title = "Home",
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {
        Navigator(ExploreScreen()) {
            FadeTransition(it)
        }
    }
}



