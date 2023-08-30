package io.silv.manga.repository_usecases

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val repositoryUseCaseModule = module {

    factoryOf(::UpdateChapterList)

    factoryOf(::UpdateMangaResourceWithArt)

    factoryOf(::GetMangaResourcesById)
}

