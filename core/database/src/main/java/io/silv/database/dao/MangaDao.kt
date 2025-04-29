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
        cover_art = coalesce(cover_art, :coverArt),
        title = coalesce(:title, title),
        version = coalesce(:version, version),
        updated_at = coalesce(:updatedAt, updated_at),
        description = coalesce(:description, description),
        alternate_titles = coalesce(:alternateTitles, alternate_titles),
        original_language = coalesce(:originalLanguage, original_language),
        available_translated_languages = coalesce(:availableTranslatedLanguages, available_translated_languages),
        status = coalesce(:status, status),
        tag_to_id = coalesce(:tagToId, tag_to_id),
        content_rating = coalesce(:contentRating, content_rating),
        last_volume = coalesce(:lastVolume, last_volume),
        last_chapter = coalesce(:lastChapter, last_chapter),
        publication_demographic = coalesce(:publicationDemographic, publication_demographic),
        year = coalesce(:year, year),
        latest_uploaded_chapter = coalesce(:latestUploadedChapter, latest_uploaded_chapter),
        authors = coalesce(:authors, authors),
        artists = coalesce(:artists, artists),
        cover_last_modified = coalesce(cover_last_modified, :coverLastModified)
        WHERE id = :mangaId
        """
    )
    abstract suspend fun updateFields(
        mangaId: String?,
        coverArt: String?,
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
        coverLastModified: Long?,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(manga: MangaEntity): Long

    @Update
    abstract suspend fun update(manga: MangaEntity)

    @Query("SELECT MAX(last_synced_for_updates) FROM manga WHERE favorite")
    abstract fun observeLastSyncedTime(): Flow<LocalDateTime?>

    @Transaction
    suspend fun upsert(
        entity: MangaEntity,
        checkIfCoverNeedsUpdate: (MangaEntity) -> Long?
    ) {
        if (insert(entity) == -1L) {
            val prev = getById(entity.id) ?: return
            val coverLastModified = checkIfCoverNeedsUpdate(prev)
            update(
                prev.copy(
                    coverLastModified = coverLastModified ?: prev.coverLastModified,
                    coverArt = entity.coverArt,
                    title = entity.title,
                    alternateTitles = entity.alternateTitles,
                    status = entity.status,
                    availableTranslatedLanguages = entity.availableTranslatedLanguages,
                    originalLanguage = entity.originalLanguage,
                    publicationDemographic = entity.publicationDemographic,
                    description = entity.description,
                    tagToId = entity.tagToId,
                    contentRating = entity.contentRating,
                )
            )
        }
    }

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
