package io.silv.data.di

import io.silv.data.author.AuthorListRepository
import io.silv.data.author.AuthorListRepositoryImpl
import io.silv.data.chapter.ChapterEntityRepository
import io.silv.data.chapter.ChapterImageRepository
import io.silv.data.chapter.ChapterImageRepositoryImpl
import io.silv.data.chapter.OfflineFirstChapterInfoRepository
import io.silv.data.manga.FilteredYearlyMangaRepository
import io.silv.data.manga.FilteredYearlyMangaRepositoryImpl
import io.silv.data.manga.MangaUpdateRepository
import io.silv.data.manga.MangaUpdateRepositoryImpl
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SavedMangaRepositoryImpl
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.data.manga.SeasonalMangaRepositoryImpl
import io.silv.data.tags.TagRepository
import io.silv.data.tags.TagRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mangaModule = module {


    singleOf(::SavedMangaRepositoryImpl)  {
        bind<SavedMangaRepository>()
    }

    singleOf(::OfflineFirstChapterInfoRepository) {
        bind<ChapterEntityRepository>()
    }

    singleOf(::SeasonalMangaRepositoryImpl) {
        bind<SeasonalMangaRepository>()
    }

    singleOf(::FilteredYearlyMangaRepositoryImpl) {
        bind<FilteredYearlyMangaRepository>()
    }


    singleOf(::TagRepositoryImpl) {
        bind<TagRepository>()
    }

    singleOf(::AuthorListRepositoryImpl) {
        bind<AuthorListRepository>()
    }


    singleOf(::ChapterImageRepositoryImpl) {
        bind<ChapterImageRepository>()
    }

    singleOf(::MangaUpdateRepositoryImpl) {
        bind<MangaUpdateRepository>()
    }
}