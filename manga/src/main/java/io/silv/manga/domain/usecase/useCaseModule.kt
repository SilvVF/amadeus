package io.silv.manga.domain.usecase

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factory {
        UpdateResourceChapterWithArt.defaultImpl()
    }

    factory {
        UpdateChapterWithArt.defaultImpl()
    }

    factoryOf(::CombineMangaChapterInfo)
}

