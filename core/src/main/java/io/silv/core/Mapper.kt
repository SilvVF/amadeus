package io.silv.core


fun interface Mapper<F, T> {
    fun map(from: F): T
}

fun interface IndexedMapper<F, T> {
    fun map(index: Int, from: F): T
}