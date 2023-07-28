package io.silv.manga.domain.usecase

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factory {
        UpdateResourceChapterWithArt.defaultImpl(
            popularMangaResourceDao = get(),
            recentMangaResourceDao = get(),
            searchMangaResourceDao = get(),
            filteredMangaResourceDao = get(),
            seasonalMangaResourceDao = get()
        )
    }

    factory {
        UpdateChapterWithArt.defaultImpl()
    }

    factoryOf(::CombineMangaChapterInfo)

    factory {
        GetMangaResourceById.defaultImpl(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
}

