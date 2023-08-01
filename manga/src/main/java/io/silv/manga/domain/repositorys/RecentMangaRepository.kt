package io.silv.manga.domain.repositorys

import io.silv.manga.domain.repositorys.base.PaginatedResourceRepository
import io.silv.manga.local.entity.RecentMangaResource

interface RecentMangaRepository : PaginatedResourceRepository<RecentMangaResource, Any?>

