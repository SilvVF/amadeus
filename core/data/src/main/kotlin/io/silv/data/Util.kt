package io.silv.data

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DateTimeAsLongSerializer : KSerializer<kotlinx.datetime.LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "Date",
        PrimitiveKind.LONG
    )

    override fun serialize(
        encoder: Encoder,
        value: kotlinx.datetime.LocalDateTime,
    ) {
        encoder.encodeLong(value.toInstant(TimeZone.currentSystemDefault()).epochSeconds)
    }

    override fun deserialize(decoder: Decoder): kotlinx.datetime.LocalDateTime {
        return Instant.fromEpochSeconds(decoder.decodeLong()).toLocalDateTime(
            TimeZone.currentSystemDefault()
        )
    }
}
