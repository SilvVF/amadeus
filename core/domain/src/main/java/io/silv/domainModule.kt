package io.silv

import io.silv.domain.chapter.interactor.ChapterHandler
import io.silv.domain.chapter.interactor.GetChapter
import io.silv.domain.manga.SubscribeToPagingData
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.interactor.GetMangaWithChapters
import io.silv.domain.manga.interactor.MangaHandler
import io.silv.domain.search.RecentSearchHandler
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule =
    module {

        factoryOf(::RecentSearchHandler)

        factoryOf(::GetManga)

        factoryOf(::GetChapter)

        factoryOf(::GetMangaWithChapters)

        factoryOf(::SubscribeToPagingData)

        factoryOf(::MangaHandler)

        factoryOf(::ChapterHandler)
    }
