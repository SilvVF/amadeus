package io.silv.model

import androidx.compose.runtime.Stable

@Stable
data class DomainTag(
    val group: String,
    val name: String,
    val id: String,
)