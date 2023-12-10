package io.silv.common.time

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import kotlin.time.toKotlinDuration

fun localDateTimeNow() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun epochSeconds() = Clock.System.now().epochSeconds

fun timeZone() = TimeZone.currentSystemDefault()

fun timeNow() = Clock.System.now().toLocalDateTime(timeZone())

fun timeStringMinus(duration: kotlin.time.Duration): String {
    return Clock.System.now()
        .minus(
            duration
        )
        .toString()
        .replace(":", "%3A")
        .takeWhile {
            it != '.'
        }
}

fun timeStringMinus(duration: Duration): String {
    return Clock.System.now()
        .minus(
            duration.toKotlinDuration()
        )
        .toString()
        .replace(":", "%3A")
        .takeWhile {
            it != '.'
        }
}
fun LocalDateTime.toMangaDexTimeString(): String {
    return this
        .toString()
        .replace(":", "%3A")
        .takeWhile {
            it != '.'
        }
}

fun String.parseMangaDexTimeToDateTime(): LocalDateTime {
    // "2021-10-10T23:19:03+00:00",
    val text = this.replaceAfter('+', "").dropLast(1)
    return LocalDateTime.parse(text)
}

operator fun LocalDateTime.minus(localDateTime: LocalDateTime): kotlin.time.Duration {
    return this.toInstant(timeZone())
        .minus(
            localDateTime.toInstant(timeZone())
        )
}
