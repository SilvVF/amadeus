package io.silv.amadeus.domain.models


data class Volume(
    val number: Int,
    val count: Int,
    val chapters: List<Chapter>,
) {

    data class Chapter(
        val id: String,
        val others: List<String>
    )
}