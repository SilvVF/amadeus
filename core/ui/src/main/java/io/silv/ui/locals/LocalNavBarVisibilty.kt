package io.silv.ui.locals

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.channels.Channel

val LocalNavBarVisibility = staticCompositionLocalOf<Channel<Boolean>> { error("Channel was not provided yet") }