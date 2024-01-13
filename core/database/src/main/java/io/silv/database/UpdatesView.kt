package io.silv.database

import androidx.room.DatabaseView
import kotlinx.datetime.LocalDateTime

@DatabaseView("""
    SELECT 
        manga.id AS mangaId,
        manga.title AS mangaTitle,
        manga.favorite,
        manga.cover_art AS coverArt,
        manga.cover_last_modified AS coverLastModified,
        manga.saved_at_local AS savedAtLocal, 
        chapters.id AS chapterId,
        chapters.scanlator AS scanlator,
        chapters.last_page_read == chapters.pages AS read,
        chapters.last_page_read AS lastPageRead,
        chapters.bookmarked,
        chapters.updated_at AS chapterUpdatedAt,
        chapters.title As chapterName,
        chapters.chapter_number AS chapterNumber
    FROM manga 
    JOIN chapters ON manga_id = manga.id
    WHERE manga.favorite
    AND chapters.updated_at > manga.saved_at_local
    ORDER BY chapters.updated_at DESC;
 """)
data class UpdatesView(
    val mangaId: String,
    val mangaTitle: String,
    val chapterId: String,
    val chapterName: String,
    val scanlator: String?,
    val read: Boolean,
    val bookmarked: Boolean,
    val lastPageRead: Long,
    val favorite: Boolean,
    val coverArt: String,
    val savedAtLocal: LocalDateTime,
    val chapterUpdatedAt: LocalDateTime,
    val coverLastModified: Long,
    val chapterNumber: Double,
)