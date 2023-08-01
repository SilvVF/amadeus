package io.silv.manga.domain.repositorys.tags

import io.silv.manga.local.entity.TagEntity
import io.silv.manga.sync.Syncable
import kotlinx.coroutines.flow.Flow

interface TagRepository: Syncable {

    fun allTags(): Flow<List<TagEntity>>
}