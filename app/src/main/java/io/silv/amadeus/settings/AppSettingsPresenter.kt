package io.silv.amadeus.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.datastore.preferences.core.Preferences
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.log.logcat
import io.silv.common.model.AppTheme
import io.silv.common.model.AutomaticUpdatePeriod
import io.silv.common.model.CardType
import io.silv.datastore.Keys
import io.silv.datastore.SettingsStore
import io.silv.sync.MangaSyncPeriodicWorkName
import io.silv.sync.workers.MangaSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppSettingsEvent {
    data class ChangeTheme(val theme: AppTheme): AppSettingsEvent
    data class ChangeUpdateInterval(val interval: AutomaticUpdatePeriod): AppSettingsEvent
}

data class AppSettings(
    val automaticUpdatePeriod: AutomaticUpdatePeriod = AutomaticUpdatePeriod.Off,
    val theme: AppTheme = AppTheme.SYSTEM_DEFAULT,
    val events: (AppSettingsEvent) -> Unit = {}
)

class AppSettingsPresenter(
    parentScope: CoroutineScope,
    private val workManager: WorkManager,
    private val store: SettingsStore,
) {

    private val scope = CoroutineScope(parentScope.coroutineContext + AndroidUiDispatcher.Main)

    val state = scope.launchMolecule(RecompositionMode.ContextClock) {
        present()
    }

    @Composable
    fun present(): AppSettings {

        val scope = rememberCoroutineScope()

        val theme by store.theme.collectAsState()
        val automaticUpdatePeriod by store.updateInterval.collectAsState()

        fun <T> editSettings(key: Preferences.Key<T>, value: T) {
            scope.launch {
                store.edit { prefs ->
                    prefs[key] = value
                }
            }
        }

        return AppSettings(
            automaticUpdatePeriod,
            theme,
        ) { event ->
            when(event) {
                is AppSettingsEvent.ChangeTheme -> editSettings(Keys.theme, event.theme.ordinal)
                is AppSettingsEvent.ChangeUpdateInterval -> {
                    scope.launch {
                        val persisted = store.edit { prefs ->
                            prefs[Keys.updateInterval] = event.interval.ordinal
                        }
                        if (persisted != null) {
                            MangaSyncWorker.syncWorkRequest(event.interval)
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
            }
        }
    }

}
