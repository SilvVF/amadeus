package io.silv.amadeus.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class DomainVolume(
    val mangaId: String,
    val number: Int,
    val count: Int,
    val chapters: List<DomainChapter>,
) : Parcelable

@Parcelize
data class DomainChapter(
    val mangaId: String,
    val title: String? = null,
    val volume: String? = null,
    val chapter: String? = null,
    val pages: Int = 0,
    val translatedLanguage: String,
    val uploader: String,
    val externalUrl: String? = null,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val readableAt: String,
): Parcelable
