package io.silv.database.entity.list

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserList")
data class UserListEntity(

    @ColumnInfo("list_id")
    val listId: String,

    @ColumnInfo("created_by")
    val createdBy: String,

    val version: Int,
    val mangaIds: List<String>,
    val name: String,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)