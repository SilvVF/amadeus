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
import io.silv.common.time.localDateTimeNow
import io.silv.database.entity.manga.MangaEntity
import io.silv.database.entity.manga.MangaEntityWithChapters
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
abstract class MangaDao {

    @Query(
        """
        UPDATE manga SET
        cover_art = :coverArt,
        title = :title,
        version = :version,
        updated_at = :updatedAt,
        description = :description,
        alternate_titles = :alternateTitles,
        originalLanguage = :originalLanguage,
        available_translated_languages = :availableTranslatedLanguages,
        status = :status,
        tag_to_id = :tagToId,
        content_rating = :contentRating,
        last_volume = :lastVolume,
        last_chapter = :lastChapter,
        publication_demographic = :publicationDemographic,
        year = :year,
        latest_uploaded_chapter = :latestUploadedChapter,
        authors = :authors,
        artists = :artists
        WHERE id = :mangaId
        """
    )
    protected abstract suspend fun updateIfExists(
        mangaId: String,
        coverArt: String,
        title: String,
        version: Int,
        updatedAt: LocalDateTime,
        description: String,
        alternateTitles: Map<String, String>,
        originalLanguage: String,
        availableTranslatedLanguages: List<String>,
        status: Status,
        tagToId: Map<String, String>,
        contentRating: ContentRating,
        lastVolume: Int,
        lastChapter: Long,
        publicationDemographic: PublicationDemographic?,
        year: Int,
        latestUploadedChapter: String?,
        authors: List<String>,
        artists: List<String>,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(manga: MangaEntity): Long

    @Update
    abstract suspend fun update(manga: MangaEntity)

    @Update
    suspend fun upsert(
        id: String,
        coverArt: String?,
        title: String,
        version: Int,
        createdAt: LocalDateTime,
        updatedAt: LocalDateTime,
        favorite: Boolean?,
        description: String,
        alternateTitles: Map<String, String>,
        originalLanguage: String,
        availableTranslatedLanguages: List<String>,
        status: Status,
        tagToId: Map<String, String>,
        contentRating: ContentRating,
        lastVolume: Int,
        lastChapter: Long,
        publicationDemographic: PublicationDemographic?,
        savedAtLocal: LocalDateTime = localDateTimeNow(),
        year: Int,
        latestUploadedChapter: String?,
        authors: List<String>,
        artists: List<String>,
        progressState: ProgressState?,
        readingStatus: ReadingStatus?,
        doBeforeUpdate: (MangaEntity) -> Unit
    ) {
        if (
            coverArt != null &&
            insert(
                MangaEntity(
                    id, coverArt, title, version, createdAt, updatedAt,
                    favorite == true, description, alternateTitles,
                    originalLanguage, availableTranslatedLanguages, status, tagToId,
                    contentRating, lastVolume, lastChapter, publicationDemographic, savedAtLocal,
                    year, latestUploadedChapter, authors, artists,
                    progressState ?: ProgressState.NotStarted, readingStatus ?: ReadingStatus.None
                )
            ) == -1L
        ) {
            runCatching {
                doBeforeUpdate(
                    getById(id)!!
                )
            }
            updateIfExists(
                mangaId = id,
                coverArt = coverArt,
                title = title,
                version = version,
                updatedAt = updatedAt,
                description = description,
                alternateTitles = alternateTitles,
                originalLanguage = originalLanguage,
                availableTranslatedLanguages = availableTranslatedLanguages,
                status = status,
                tagToId = tagToId,
                contentRating = contentRating,
                lastVolume = lastVolume,
                lastChapter = lastChapter,
                publicationDemographic = publicationDemographic,
                year = year,
                latestUploadedChapter = latestUploadedChapter,
                authors = authors,
                artists = artists,
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(mangas: List<MangaEntity>)

    @Delete
    abstract suspend fun delete(manga: MangaEntity)

    @Query("SELECT * FROM MANGA WHERE id = :id LIMIT 1")
    abstract suspend fun getById(id: String): MangaEntity?

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
    @Query("SELECT * FROM MANGA WHERE id = :id")
    abstract fun observeMangaWithChaptersById(id: String): Flow<MangaEntityWithChapters>
}
