package io.silv.manga.domain.repositorys

import io.silv.manga.domain.repositorys.base.PaginatedResourceRepository
import io.silv.manga.local.entity.PopularMangaResource

interface PopularMangaRepository: PaginatedResourceRepository<PopularMangaResource, Any?>

