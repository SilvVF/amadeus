package io.silv.data.manga

import io.silv.database.entity.manga.resource.TempMangaResource

interface TempMangaRepository {

    suspend fun update(id: String, update: (TempMangaResource) -> TempMangaResource)

    suspend fun createTempResource(id: String): Boolean
}

