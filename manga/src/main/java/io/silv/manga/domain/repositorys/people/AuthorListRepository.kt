package io.silv.manga.domain.repositorys.people

import io.silv.manga.domain.models.DomainAuthor
import kotlinx.coroutines.flow.Flow

interface AuthorListRepository {

    fun getAuthorList(query: String?): Flow<QueryResult<List<DomainAuthor>>>
}