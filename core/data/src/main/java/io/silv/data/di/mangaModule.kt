package io.silv.data.di

import io.silv.data.author.AuthorListRepository
import io.silv.data.author.AuthorListRepositoryImpl
import io.silv.data.chapter.ChapterEntityRepository
import io.silv.data.chapter.ChapterImageRepository
import io.silv.data.chapter.ChapterImageRepositoryImpl
import io.silv.data.chapter.OfflineFirstChapterInfoRepository
import io.silv.data.manga.FilteredMangaRepository
import io.silv.data.manga.FilteredMangaRepositoryImpl
import io.silv.data.manga.FilteredYearlyMangaRepository
import io.silv.data.manga.FilteredYearlyMangaRepositoryImpl
import io.silv.data.manga.MangaUpdateRepository
import io.silv.data.manga.MangaUpdateRepositoryImpl
import io.silv.data.manga.PopularMangaRepository
import io.silv.data.manga.PopularMangaRepositoryImpl
import io.silv.data.manga.QuickSearchMangaRepository
import io.silv.data.manga.QuickSearchMangaRepositoryImpl
import io.silv.data.manga.RecentMangaRepository
import io.silv.data.manga.RecentMangaRepositoryImpl
import io.silv.data.manga.SavedMangaRepository
import io.silv.data.manga.SavedMangaRepositoryImpl
import io.silv.data.manga.SearchMangaRepository
import io.silv.data.manga.SearchMangaRepositoryImpl
import io.silv.data.manga.SeasonalMangaRepository
import io.silv.data.manga.SeasonalMangaRepositoryImpl
import io.silv.data.manga.TempMangaRepository
import io.silv.data.manga.TempMangaRepositoryImpl
import io.silv.data.tags.TagRepository
import io.silv.data.tags.TagRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val mangaModule = module {

    singleOf(::RecentMangaRepositoryImpl) withOptions {
        bind<RecentMangaRepository>()
    }

    singleOf(::PopularMangaRepositoryImpl) withOptions {
        bind<PopularMangaRepository>()
    }

    singleOf(::SearchMangaRepositoryImpl) withOptions {
        bind<SearchMangaRepository>()
    }

    singleOf(::SavedMangaRepositoryImpl) withOptions {
        bind<SavedMangaRepository>()
    }

    singleOf(::OfflineFirstChapterInfoRepository) withOptions {
        bind<ChapterEntityRepository>()
    }

    singleOf(::SeasonalMangaRepositoryImpl) withOptions {
        bind<SeasonalMangaRepository>()
    }

    singleOf(::FilteredMangaRepositoryImpl) withOptions {
        bind<FilteredMangaRepository>()
    }

    singleOf(::FilteredYearlyMangaRepositoryImpl) withOptions {
        bind<FilteredYearlyMangaRepository>()
    }


    singleOf(::TagRepositoryImpl) withOptions {
        bind<TagRepository>()
    }

    singleOf(::AuthorListRepositoryImpl) withOptions {
        bind<AuthorListRepository>()
    }

    singleOf(::QuickSearchMangaRepositoryImpl) withOptions {
        bind<QuickSearchMangaRepository>()
    }

    singleOf(::ChapterImageRepositoryImpl) withOptions {
        bind<ChapterImageRepository>()
    }

    singleOf(::TempMangaRepositoryImpl) withOptions {
        bind<TempMangaRepository>()
    }

    singleOf(::MangaUpdateRepositoryImpl) withOptions {
        bind<MangaUpdateRepository>()
    }
}