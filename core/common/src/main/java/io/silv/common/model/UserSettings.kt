package io.silv.common.model

import androidx.compose.runtime.Stable
import io.silv.common.model.AppTheme
import io.silv.common.model.AutomaticUpdatePeriod
import kotlinx.serialization.Serializable

@Stable
data class UserSettings(
    val updateInterval: AutomaticUpdatePeriod = AutomaticUpdatePeriod.Off,
    val theme: AppTheme = AppTheme.DYNAMIC_COLOR_DEFAULT
)