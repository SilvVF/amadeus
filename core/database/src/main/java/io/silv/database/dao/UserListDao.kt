package io.silv.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.silv.database.entity.list.UserListEntity
import io.silv.database.entity.manga.MangaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserList(userList: UserListEntity)

    @Query("SELECT * FROM UserList")
    fun observeSeasonalLists(): Flow<List<UserListEntity>>

    @Query("SELECT * FROM UserList WHERE list_id = :listId LIMIT 1")
    fun getUserListByListId(listId: String): UserListEntity?

    @Update
    fun update(userList: UserListEntity)

    @Query("DELETE FROM UserList")
    suspend fun clear()

    @Query("""
        SELECT * FROM UserList
        JOIN MANGA ON UserList.mangaIds
        WHERE UserList.id = :id
    """)
    fun observeUserListWithManga(id: String): Flow<Map<UserListEntity, List<MangaEntity>>>

    @Query("""
        SELECT * FROM UserList
        JOIN MANGA ON UserList.mangaIds
    """)
    fun observeUserListsWithManga(): Flow<Map<UserListEntity, List<MangaEntity>>>
}



