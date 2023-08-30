package io.silv.manga.repositorys.manga

import io.silv.manga.local.entity.manga_resource.TempMangaResource

interface TempMangaRepository {

    suspend fun update(id: String, update: (TempMangaResource) -> TempMangaResource)

    suspend fun createTempResource(id: String): Boolean
}

