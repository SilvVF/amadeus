package io.silv.data.di

import io.silv.data.author.AuthorListRepositoryImpl
import io.silv.data.chapter.ChapterRepositoryImpl
import io.silv.data.manga.MangaPagingSourceFactoryImpl
import io.silv.data.manga.MangaRepositoryImpl
import io.silv.data.manga.SeasonalMangaRepositoryImpl
import io.silv.data.manga.YearlyTopMangaFetcher
import io.silv.data.tags.TagRepositoryImpl
import io.silv.domain.AuthorListRepository
import io.silv.domain.TagRepository
import io.silv.domain.chapter.repository.ChapterRepository
import io.silv.domain.manga.MangaPagingSourceFactory
import io.silv.domain.manga.repository.MangaRepository
import io.silv.domain.manga.repository.SeasonalMangaRepository
import io.silv.domain.manga.repository.TopYearlyFetcher
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mangaModule = module {

    singleOf(::ChapterRepositoryImpl) {
        bind<ChapterRepository>()
    }

    singleOf(::SeasonalMangaRepositoryImpl) {
        bind<SeasonalMangaRepository>()
    }

    singleOf(::YearlyTopMangaFetcher) {
        bind<TopYearlyFetcher>()
    }

    singleOf(::TagRepositoryImpl) {
        bind<TagRepository>()
    }

    singleOf(::AuthorListRepositoryImpl) {
        bind<AuthorListRepository>()
    }

    singleOf(::MangaRepositoryImpl) {
        bind<MangaRepository>()
    }

    singleOf(::MangaPagingSourceFactoryImpl) {
        bind<MangaPagingSourceFactory>()
    }
}