package io.silv.data.di

import io.silv.data.author.AuthorListRepository
import io.silv.data.author.AuthorListRepositoryImpl
import io.silv.data.chapter.ChapterRepository
import io.silv.data.chapter.ChapterRepositoryImpl
import io.silv.data.manga.MangaUpdateRepository
import io.silv.data.manga.MangaUpdateRepositoryImpl
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SavedMangaRepositoryImpl
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.data.manga.SeasonalMangaRepositoryImpl
import io.silv.data.manga.SourceMangaRepository
import io.silv.data.manga.SourceMangaRepositoryImpl
import io.silv.data.tags.TagRepository
import io.silv.data.tags.TagRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mangaModule = module {


    singleOf(::SavedMangaRepositoryImpl)  {
        bind<SavedMangaRepository>()
    }

    singleOf(::ChapterRepositoryImpl) {
        bind<ChapterRepository>()
    }

    singleOf(::SeasonalMangaRepositoryImpl) {
        bind<SeasonalMangaRepository>()
    }

    singleOf(::TagRepositoryImpl) {
        bind<TagRepository>()
    }

    singleOf(::AuthorListRepositoryImpl) {
        bind<AuthorListRepository>()
    }

    singleOf(::SourceMangaRepositoryImpl) {
        bind<SourceMangaRepository>()
    }


    singleOf(::MangaUpdateRepositoryImpl) {
        bind<MangaUpdateRepository>()
    }
}