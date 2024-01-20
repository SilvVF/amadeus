package io.silv.ui

import io.silv.common.PrefsConverter
import io.silv.ui.composables.CardType

enum class ReaderLayout {
    PagedRTL,
    PagedLTR,
    Vertical
}

object Converters {
    val CardTypeToStringConverter = PrefsConverter.create<CardType, String>(
       convertFrom = { CardType.valueOf(it) },
       convertTo = { it.toString() }
    )

    val LayoutDirectionConverter = PrefsConverter.create<ReaderLayout, Int>(
        convertTo = { dir -> dir.ordinal },
        convertFrom = { v -> ReaderLayout.entries[v] }
    )
}