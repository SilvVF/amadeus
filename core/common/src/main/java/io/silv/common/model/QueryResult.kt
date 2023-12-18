package io.silv.common.model

sealed interface QueryResult<out T> {
    object Loading : QueryResult<Nothing>

    data class Done<T>(val result: T) : QueryResult<T>
}
