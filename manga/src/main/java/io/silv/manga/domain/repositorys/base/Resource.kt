package io.silv.manga.domain.repositorys.base

sealed class Resource<out T> {
    object Loading: Resource<Nothing>()
    data class Success<T>(val result: T): Resource<T>()
    data class Failure<T>(val message: String, val result: T?): Resource<T>()
}
