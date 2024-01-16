package io.silv.ui

import androidx.compose.ui.unit.LayoutDirection
import io.silv.common.PrefsConverter
import io.silv.common.model.ReaderOrientation
import io.silv.ui.composables.CardType

object Converters {
    val CardTypeToStringConverter = PrefsConverter.create<CardType, String>(
       convertFrom = { CardType.valueOf(it) },
       convertTo = { it.toString() }
    )

    val LayoutDirectionConverter = PrefsConverter.create<LayoutDirection, Int>(
        convertTo = { dir -> dir.ordinal },
        convertFrom = {  v -> if (v == 1) LayoutDirection.Rtl else LayoutDirection.Ltr  }
    )

    val OrientationConverter = PrefsConverter.create<ReaderOrientation, String>(
        convertTo = { it.toString() },
        convertFrom = {
            ReaderOrientation.valueOf(it)
        }
    )
}