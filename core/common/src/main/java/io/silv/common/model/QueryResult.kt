package io.silv.common.model

import androidx.compose.runtime.Stable

@Stable
sealed interface QueryResult<out T> {
    data object Loading : QueryResult<Nothing>

    data class Done<T>(val result: T) : QueryResult<T>
}
