package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.silv.common.model.ContentRating
import io.silv.common.model.ProgressState
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.ReadingStatus
import io.silv.common.model.Status
import io.silv.database.entity.manga.MangaEntity
import io.silv.database.entity.manga.MangaEntityWithChapters
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
abstract class MangaDao {

    @Query(
        """
        UPDATE manga SET
        cover_art = COALESCE(:coverArt, cover_art),
        title = COALESCE(:title, title),
        version = COALESCE(:version, version),
        updated_at = COALESCE(:updatedAt, updated_at),
        description = COALESCE(:description, description),
        alternate_titles = COALESCE(:alternateTitles, alternate_titles),
        original_language = COALESCE(:originalLanguage, original_language),
        available_translated_languages = COALESCE(:availableTranslatedLanguages, available_translated_languages),
        status = COALESCE(:status, status),
        tag_to_id = COALESCE(:tagToId, tag_to_id),
        content_rating = COALESCE(:contentRating, content_rating),
        last_volume = COALESCE(:lastVolume, last_volume),
        last_chapter = COALESCE(:lastChapter, last_chapter),
        publication_demographic = COALESCE(:publicationDemographic, publication_demographic),
        year = COALESCE(:year, year),
        latest_uploaded_chapter = COALESCE(:latestUploadedChapter, latest_uploaded_chapter),
        authors = COALESCE(:authors, authors),
        artists = COALESCE(:artists, artists),
        cover_last_modified = COALESCE(:coverLastModified, cover_last_modified),
        favorite = COALESCE(:favorite, favorite),
        reading_status = COALESCE(:readingStatus, reading_status),
        progress_state = COALESCE(:progressState, progress_state)
        WHERE id = :mangaId
        """
    )
    abstract suspend fun updateFields(
        mangaId: String,
        coverArt: String?,
        favorite: Boolean?,
        title: String?,
        version: Int?,
        updatedAt: LocalDateTime?,
        description: String?,
        alternateTitles: Map<String, String>?,
        originalLanguage: String?,
        availableTranslatedLanguages: List<String>?,
        status: Status?,
        tagToId: Map<String, String>?,
        contentRating: ContentRating?,
        lastVolume: Int?,
        lastChapter: Long?,
        publicationDemographic: PublicationDemographic?,
        year: Int?,
        latestUploadedChapter: String?,
        authors: List<String>?,
        artists: List<String>?,
        progressState: ProgressState?,
        readingStatus: ReadingStatus?,
        coverLastModified: Long?,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(manga: MangaEntity): Long

    @Update
    abstract suspend fun update(manga: MangaEntity)

    @Query("SELECT MAX(last_synced_for_updates) FROM manga WHERE favorite")
    abstract fun observeLastSyncedTime(): Flow<LocalDateTime?>

    @Delete
    abstract suspend fun delete(manga: MangaEntity)

    @Query(
        """
        DELETE 
        FROM manga
        WHERE NOT manga.favorite  
        AND (
            NOT EXISTS (
                SELECT 1
                FROM chapters
                WHERE manga.id = chapters.manga_id 
                AND(chapters.last_page_read IS NOT NULL OR chapters.bookmarked = 1)
            )
            OR NOT EXISTS (
                SELECT 1
                FROM chapters
                WHERE manga.id = chapters.manga_id
            )
        )
    """
    )
    abstract suspend fun deleteUnused()

    @Query(
        """
        SELECT COUNT(DISTINCT manga.id)
        FROM manga
        WHERE NOT manga.favorite 
        AND (
            NOT EXISTS (
                SELECT 1
                FROM chapters
                WHERE manga.id = chapters.manga_id 
                AND(chapters.last_page_read IS NOT NULL OR chapters.bookmarked = 1)
            )
            OR NOT EXISTS (
                SELECT 1
                FROM chapters
                WHERE manga.id = chapters.manga_id
            )
        )
    """
    )
    abstract fun observeUnusedMangaCount(): Flow<Int>

    @Query("SELECT * FROM MANGA WHERE id = :id LIMIT 1")
    abstract suspend fun getById(id: String): MangaEntity?

    @Query("SELECT * FROM MANGA WHERE title LIKE :name LIMIT 1")
    abstract suspend fun getMangaByTitle(name: String): MangaEntity?

    @Query("SELECT * FROM MANGA WHERE id in (:ids)")
    abstract suspend fun getByIds(ids: List<String>): List<MangaEntity>

    @Query("SELECT * FROM MANGA WHERE id in (:ids)")
    abstract fun observeByIds(ids: List<String>): Flow<List<MangaEntity>>

    @Query("SELECT * FROM MANGA WHERE id = :id LIMIT 1")
    abstract fun observeById(id: String): Flow<MangaEntity?>

    @Query("SELECT * FROM MANGA")
    abstract suspend fun getAll(): List<MangaEntity>

    @Query("SELECT * FROM MANGA")
    abstract fun observeAll(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM MANGA WHERE favorite")
    abstract suspend fun getLibraryManga(): List<MangaEntity>

    @Query("SELECT * FROM MANGA WHERE favorite")
    abstract fun observeLibraryManga(): Flow<List<MangaEntity>>

    @Transaction
    @Query("SELECT * FROM MANGA WHERE favorite")
    abstract fun observeLibraryMangaWithChapters(): Flow<List<MangaEntityWithChapters>>

    @Transaction
    @Query("SELECT * FROM MANGA WHERE favorite")
    abstract suspend fun getLibraryMangaWithChapters(): List<MangaEntityWithChapters>

    @Transaction
    @Query("SELECT * FROM MANGA WHERE id = :id")
    abstract fun observeMangaWithChaptersById(id: String): Flow<MangaEntityWithChapters>

    @Transaction
    @Query("SELECT * FROM MANGA WHERE id = :id")
    abstract suspend fun getMangaWithChaptersById(id: String): MangaEntityWithChapters?
}
