package io.silv.model

import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant

@Stable
data class RecentSearch(
    val query: String,
    val date: Instant,
)
