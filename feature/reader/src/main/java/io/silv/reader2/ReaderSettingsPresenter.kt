package io.silv.reader2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.preferences.core.Preferences
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import io.silv.common.model.ReaderLayout
import io.silv.datastore.SettingsStore
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.cash.molecule.AndroidUiDispatcher
import io.silv.datastore.Keys
import kotlinx.coroutines.launch

sealed interface ReaderSettingsEvent {
    data class ChangeLayout(val layout: ReaderLayout) : ReaderSettingsEvent
    data class ChangeColor(val color: Color) : ReaderSettingsEvent
    data object ToggleFullscreen : ReaderSettingsEvent
    data object ToggleShowPageNumber : ReaderSettingsEvent
}

@Stable
data class ReaderSettings(
    val layout: ReaderLayout = ReaderLayout.PagedRTL,
    val color: Color = Color.Black,
    val fullscreen: Boolean = false,
    val showPageNumber: Boolean = true,
    val removeAfterReadSlots: Int = -1,
    val automaticBackground: Boolean = false,
    val dualPageSplit: Boolean = false,
    val dualPageInvert: Boolean = false,
    val dualPageRotateToFit: Boolean = false,
    val dualPageRotateToFitInvert: Boolean = false,
    val events: (ReaderSettingsEvent) -> Unit = {}
)

class ReaderSettingsPresenter(
    parentScope: CoroutineScope,
    private val store: SettingsStore,
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + AndroidUiDispatcher.Main)

    val state = scope.launchMolecule(RecompositionMode.ContextClock) {
        present()
    }

    @Composable
    fun present(): ReaderSettings {
        val scope = rememberCoroutineScope()

        val layout by store.layoutDirection.collectAsState()
        val colorInt by store.backgroundColor.collectAsState()
        val fullscreen by rememberUpdatedState(store.fullscreen.collectAsState().value)
        val showPageNumber by rememberUpdatedState(store.showPageNumber.collectAsState().value)
        val removeAfterReadSlots by store.removeAfterReadSlots.collectAsState()

        var automaticBackground by remember { mutableStateOf(false) }
        var dualPageSplit by remember { mutableStateOf(false) }
        var dualPageInvert by remember {
            mutableStateOf(false)
        }
        var dualPageRotateToFit by remember {
            mutableStateOf(false)
        }
        var dualPageRotateToFitInvert by remember {
            mutableStateOf(false)
        }


        val color by remember {
            derivedStateOf {
                runCatching { Color(colorInt) }.getOrDefault(Color.Black)
            }
        }

        fun <T> editSettings(
            key: Preferences.Key<T>,
            value: T
        ) {
            scope.launch {
                store.edit { prefs ->
                    prefs[key] = value
                }
            }
        }

        return ReaderSettings(
            layout = layout,
            color = color,
            fullscreen = fullscreen,
            showPageNumber = showPageNumber,
            removeAfterReadSlots = removeAfterReadSlots,
            automaticBackground = automaticBackground,
            dualPageSplit = dualPageSplit,
            dualPageInvert = dualPageInvert,
            dualPageRotateToFitInvert = dualPageRotateToFitInvert,
            dualPageRotateToFit =  dualPageRotateToFit
        ) {
            when (it) {
                is ReaderSettingsEvent.ChangeLayout -> editSettings(
                    Keys.ReaderPrefs.layoutDirection,
                    it.layout.ordinal
                )

                is ReaderSettingsEvent.ChangeColor -> editSettings(
                    Keys.ReaderPrefs.backgroundColor,
                    it.color.toArgb()
                )

                ReaderSettingsEvent.ToggleFullscreen -> editSettings(
                    Keys.ReaderPrefs.fullscreen,
                    !fullscreen
                )

                ReaderSettingsEvent.ToggleShowPageNumber -> editSettings(
                    Keys.ReaderPrefs.showPageNumber,
                    !showPageNumber
                )
            }
        }
    }
}