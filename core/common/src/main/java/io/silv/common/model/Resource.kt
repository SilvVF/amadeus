package io.silv.common.model

import androidx.compose.runtime.Stable

@Stable
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()

    data class Success<T>(val result: T) : Resource<T>()

    data class Failure<T>(val message: String, val result: T?) : Resource<T>()
}
