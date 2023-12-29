package io.silv.domain

import io.silv.model.DomainTag
import kotlinx.coroutines.flow.Flow

interface TagRepository: Syncable {

    fun allTags(): Flow<List<DomainTag>>
}