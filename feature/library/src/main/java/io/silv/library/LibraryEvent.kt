package io.silv.library

sealed interface LibraryEvent {
    data class BookmarkStatusChanged(val id: String, val bookmarked: Boolean) : LibraryEvent

    data class ReadStatusChanged(val id: String, val read: Boolean) : LibraryEvent
}
