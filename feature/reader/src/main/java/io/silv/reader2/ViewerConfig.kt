package io.silv.reader2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.silv.common.DependencyAccessor
import io.silv.common.commonDeps
import io.silv.di.dataDeps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(DependencyAccessor::class)
class PagerConfig(
    private val isHorizontal: Boolean,
    scope: CoroutineScope = CoroutineScope(commonDeps.dispatchers.default),
    dataStore: DataStore<Preferences> = dataDeps.dataStore
) : ViewerConfig(scope, dataStore) {

    var automaticBackground by mutableStateOf(false)
        private set

    var imageCropBorders by mutableStateOf(false)
        private set

    var navigateToPan by mutableStateOf(false)
        private set

    var landscapeZoom by mutableStateOf(false)
        private set

    override var navigator: ViewerNavigation by mutableStateOf(defaultNavigation())

    override fun defaultNavigation(): ViewerNavigation {
        return if(isHorizontal) {
            RightAndLeftNavigation()
        } else {
            LNavigation()
        }
    }

    override fun updateNavigation(navigationMode: Int) {
        navigator = when (navigationMode) {
            0 -> defaultNavigation()
            1 -> LNavigation()
            4 -> RightAndLeftNavigation()
            else -> defaultNavigation()
        }
    }
}

abstract class ViewerConfig(
    private val scope: CoroutineScope,
    private val dataStore: DataStore<Preferences>
) {

    var theme by mutableIntStateOf(0)
        private set

    var tappingInverted by mutableStateOf(false)
    var longTapEnabled by mutableStateOf(true)
    var usePageTransitions by mutableStateOf(false)
    var volumeKeysEnabled by mutableStateOf(false)
    var volumeKeysInverted by mutableStateOf(false)
    var alwaysShowChapterTransition by mutableStateOf(true)

    var navigationMode by mutableIntStateOf(0)
        protected set

    var forceNavigationOverlay by mutableStateOf(false)
    var navigationOverlayOnStart by mutableStateOf(false)

    var dualPageSplit by mutableStateOf(false)
        protected set

    var dualPageInvert by mutableStateOf(false)
        protected set

    var dualPageRotateToFit by mutableStateOf(false)
        protected set

    var dualPageRotateToFitInvert by mutableStateOf(false)
        protected set

    abstract var navigator: ViewerNavigation
        protected set

    protected abstract fun defaultNavigation(): ViewerNavigation

    abstract fun updateNavigation(navigationMode: Int)

    fun <T> Preferences.Key<T>.register(
        valueAssignment: (T) -> Unit,
        onChanged: (T) -> Unit = {},
    ) {
        dataStore.data.map { prefs ->
            prefs[this]
        }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach {
                valueAssignment(it)
                onChanged(it)
            }
            .launchIn(scope)
    }
}