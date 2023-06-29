package io.silv.amadeus.domain.models

data class ChapterImages (
    val images: List<Image>,
    val dataSaverImages: List<Image>
) {

    data class Image(
        val uri: String,
    )
}