package io.silv.data

import io.silv.model.DomainTag
import kotlinx.coroutines.flow.Flow

interface TagRepository: Syncable {

    fun allTags(): Flow<List<DomainTag>>
}