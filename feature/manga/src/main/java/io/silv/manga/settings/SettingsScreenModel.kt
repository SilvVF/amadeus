package io.silv.manga.settings

import cafe.adriel.voyager.core.model.StateScreenModel

class SettingsScreenModel(

): StateScreenModel<SettingsState>(SettingsState()) {


}

data class SettingsState(
    val loading: Boolean = true
)