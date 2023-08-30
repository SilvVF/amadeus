package io.silv.manga.repositorys

import io.silv.manga.repository_usecases.repositoryUseCaseModule
import io.silv.manga.repositorys.author.AuthorListRepository
import io.silv.manga.repositorys.author.AuthorListRepositoryImpl
import io.silv.manga.repositorys.chapter.ChapterEntityRepository
import io.silv.manga.repositorys.chapter.ChapterImageRepository
import io.silv.manga.repositorys.chapter.ChapterImageRepositoryImpl
import io.silv.manga.repositorys.chapter.OfflineFirstChapterInfoRepository
import io.silv.manga.repositorys.manga.FilteredMangaRepository
import io.silv.manga.repositorys.manga.FilteredMangaRepositoryImpl
import io.silv.manga.repositorys.manga.FilteredYearlyMangaRepository
import io.silv.manga.repositorys.manga.FilteredYearlyMangaRepositoryImpl
import io.silv.manga.repositorys.manga.PopularMangaRepository
import io.silv.manga.repositorys.manga.PopularMangaRepositoryImpl
import io.silv.manga.repositorys.manga.QuickSearchMangaRepository
import io.silv.manga.repositorys.manga.QuickSearchMangaRepositoryImpl
import io.silv.manga.repositorys.manga.RecentMangaRepository
import io.silv.manga.repositorys.manga.RecentMangaRepositoryImpl
import io.silv.manga.repositorys.manga.SavedMangaRepository
import io.silv.manga.repositorys.manga.SavedMangaRepositoryImpl
import io.silv.manga.repositorys.manga.SearchMangaRepository
import io.silv.manga.repositorys.manga.SearchMangaRepositoryImpl
import io.silv.manga.repositorys.manga.SeasonalMangaRepository
import io.silv.manga.repositorys.manga.SeasonalMangaRepositoryImpl
import io.silv.manga.repositorys.manga.TempMangaRepository
import io.silv.manga.repositorys.manga.TempMangaRepositoryImpl
import io.silv.manga.repositorys.tags.TagRepository
import io.silv.manga.repositorys.tags.TagRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val repositoryModule = module {

    includes(repositoryUseCaseModule)

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
}