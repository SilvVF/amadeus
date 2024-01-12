package io.silv.database.entity.history

import androidx.room.DatabaseView
import kotlinx.datetime.LocalDateTime

@DatabaseView("""
    SELECT 
        history.id AS id,
        manga.id AS mangaId,
        chapters.id AS chapterId,
        manga.title,
        manga.cover_art AS coverArt,
        manga.favorite,
        manga.saved_at_local AS mangaSavedAt,
        chapters.chapter_number AS chapter,
        chapters.volume,
        chapters.title AS name,
        chapters.last_page_read AS lastPageRead,
        chapters.pages AS pageCount,
        history.last_read AS lastRead,
        history.time_read AS timeRead,
        max_last_read.chapter_id AS maxReadAtChapterId
    FROM manga
    JOIN chapters ON manga.id = chapters.manga_id
    JOIN history ON chapters.id = history.chapter_id
   JOIN (
        SELECT chapters.manga_id, chapters.id AS chapter_id, MAX(history.last_read) AS last_read
        FROM chapters JOIN history
        ON chapters.id = history.chapter_id
        GROUP BY chapters.manga_id
    ) AS max_last_read
    ON chapters.manga_id = max_last_read.manga_id;
 """)
data class HistoryView(
    val id: Long = 0L,
    val chapterId: String,
    val favorite: Boolean,
    val lastRead: LocalDateTime,
    val timeRead: Long,
    val mangaId: String,
    val coverArt: String,
    val title: String,
    val name: String,
    val chapter: Double,
    val volume: Int,
    val lastPageRead: Int,
    val pageCount: Int,
    val mangaSavedAt: LocalDateTime,
    val maxReadAtChapterId: String
)