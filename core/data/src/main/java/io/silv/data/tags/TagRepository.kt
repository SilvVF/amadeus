package io.silv.data.tags

import io.silv.data.util.Syncable
import io.silv.database.entity.list.TagEntity
import kotlinx.coroutines.flow.Flow

interface TagRepository: Syncable {

    fun allTags(): Flow<List<TagEntity>>
}