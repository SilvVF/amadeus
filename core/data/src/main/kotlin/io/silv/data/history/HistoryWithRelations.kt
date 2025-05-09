package io.silv.data.history

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime

@Stable
data class HistoryWithRelations(
    val id: Long,
    val chapterId: String,
    val title: String,
    val chapterName: String,
    val lastRead: LocalDateTime,
    val timeRead: Long,
    val mangaId: String,
    val coverArt: String,
    val lastPage: Int,
    val pageCount: Int,
    val chapterNumber: Double,
    val volume: Int,
    val favorite: Boolean,
    val coverLastModified: Long,
) {

    val formattedTimeText: String
        get() {
            var h = lastRead.time.hour
            var isPm = false

            when (h) {
                0 -> h = 12
                12 -> isPm = true
                in 12..24 -> {
                    h -= 12
                    isPm = true
                }
            }
            val amPm = if (isPm) "PM" else "AM"
            val time = if (lastRead.time.minute < 10) {
                "0${lastRead.time.minute}"
            } else lastRead.time.minute.toString()
            return "$h:${time} $amPm"
        }
}
