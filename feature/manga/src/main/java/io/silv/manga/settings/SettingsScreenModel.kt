package io.silv.manga.settings

import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.log.logcat
import io.silv.common.model.AppTheme
import io.silv.common.model.AutomaticUpdatePeriod
import io.silv.datastore.SettingsStore
import io.silv.datastore.UserSettings
import io.silv.di.dataDeps
import io.silv.sync.MangaSyncPeriodicWorkName
import io.silv.sync.workers.MangaSyncWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsScreenModel @OptIn(DependencyAccessor::class) constructor(
    private val workManager: WorkManager = dataDeps.workManager,
    private val settingsStore: SettingsStore = dataDeps.settingsStore
): StateScreenModel<SettingsState>(SettingsState()) {

    val settingsPrefs = settingsStore.observe()
        .onEach { Log.d("Settings", it.toString()) }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

    fun changeCurrentDialog(key: String?) {
        mutableState.update {
            it.copy(
                dialogkey = key
            )
        }
    }

    fun changeAutomaticUpdatePeriod(automaticUpdatePeriod: AutomaticUpdatePeriod) {
        screenModelScope.launch {
            val persisted = settingsStore.update { prev ->
                logcat { "$prev" }
                prev.copy(updateInterval = automaticUpdatePeriod)
            }
            if (persisted) {
                MangaSyncWorker.syncWorkRequest(automaticUpdatePeriod)
                    ?.let {
                        workManager.enqueueUniquePeriodicWork(
                            MangaSyncPeriodicWorkName,
                            ExistingPeriodicWorkPolicy.UPDATE,
                            it
                        )
                    }
                    ?: workManager.cancelUniqueWork(MangaSyncPeriodicWorkName)
            }
        }
    }

    fun changeAppTheme(theme: AppTheme) {
        screenModelScope.launch {
            settingsStore.update { prev ->
                prev.copy(theme = theme)
            }
        }
    }

    companion object {
        const val UPDATE_PERIOD_KEY = "auto_update_period"
        const val FILTER_DISPLAY_KEY = "filter_display_key"
        const val LIBRARY_DISPLAY_KEY = "library_display_key"
        const val EXPLORE_DISPLAY_KEY = "explore_display_key"
    }
}

data class SettingsState(
    val dialogkey: String? = null
)