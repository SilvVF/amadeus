package io.silv.amadeus.settings

import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.combineTuple
import io.silv.datastore.SettingsStore
import io.silv.di.dataDeps
import io.silv.explore.ExploreSettings
import io.silv.explore.ExploreSettingsPresenter
import io.silv.library.LibrarySettings
import io.silv.library.LibrarySettingsPresenter
import io.silv.manga.filter.FilterSettings
import io.silv.manga.filter.FilterSettingsPresenter
import io.silv.reader2.ReaderSettings
import io.silv.reader2.ReaderSettingsPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class SettingsScreenModel @OptIn(DependencyAccessor::class) constructor(
    workManager: WorkManager = dataDeps.workManager,
    settingsStore: SettingsStore = dataDeps.settingsStore
) : ScreenModel {

    private val dialogFlow = MutableStateFlow<Dialog?>(null)

    val filtersSettings = FilterSettingsPresenter(screenModelScope, settingsStore)
    val readerSettings = ReaderSettingsPresenter(screenModelScope, settingsStore)
    val exploreSettings = ExploreSettingsPresenter(screenModelScope, settingsStore)
    val librarySettings = LibrarySettingsPresenter(screenModelScope, settingsStore)
    val appSettings = AppSettingsPresenter(screenModelScope, workManager, settingsStore)

    val state = combineTuple(
        filtersSettings.state,
        readerSettings.state,
        exploreSettings.state,
        librarySettings.state,
        appSettings.state,
        dialogFlow
    )
        .map { (filter, reader, explore, library, appSettings, dialogValue) ->
            SettingsState(
                filterSettings = filter,
                readerSettings = reader,
                exploreSettings = explore,
                librarySettings = library,
                appSettings = appSettings,
                dialog = dialogValue
            ) {
                when(it) {
                    is SettingsEvent.ChangeDialog -> dialogFlow.value = it.dialog
                }
            }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsState()
        )

    sealed interface Dialog {
        data object UpdatePeriod : Dialog
        data object Filter : Dialog
        data object Library : Dialog
        data object Explore : Dialog
    }
}

sealed interface SettingsEvent {
    data class ChangeDialog(val dialog: SettingsScreenModel.Dialog?): SettingsEvent
}

data class SettingsState(
    val appSettings: AppSettings = AppSettings(),
    val filterSettings: FilterSettings = FilterSettings(),
    val readerSettings: ReaderSettings = ReaderSettings(),
    val exploreSettings: ExploreSettings = ExploreSettings(),
    val librarySettings: LibrarySettings = LibrarySettings(),
    val dialog: SettingsScreenModel.Dialog? = null,
    val events: (SettingsEvent) -> Unit = {}
)