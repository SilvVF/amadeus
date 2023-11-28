package io.silv.common.model

enum class ProgressState {
    Finished,
    NotStarted,
    Reading
}


enum class ReadingStatus {
    None,
    Reading,
    OnHold,
    Dropped,
    PlanToRead,
    Completed,
    ReReading
}

fun ReadingStatus.string() = when(this) {
    ReadingStatus.None -> "None"
    ReadingStatus.Reading -> "Reading"
    ReadingStatus.OnHold -> "On Hold"
    ReadingStatus.Dropped -> "Dropped"
    ReadingStatus.PlanToRead -> "Plan To Read"
    ReadingStatus.Completed -> "Completed"
    ReadingStatus.ReReading -> "Re-Reading"
}