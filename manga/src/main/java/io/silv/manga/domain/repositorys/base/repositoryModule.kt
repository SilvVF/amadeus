package io.silv.manga.domain.repositorys.base

import io.silv.manga.domain.repositorys.FilteredMangaRepository
import io.silv.manga.domain.repositorys.FilteredMangaRepositoryImpl
import io.silv.manga.domain.repositorys.FilteredYearlyMangaRepository
import io.silv.manga.domain.repositorys.FilteredYearlyMangaRepositoryImpl
import io.silv.manga.domain.repositorys.PopularMangaRepository
import io.silv.manga.domain.repositorys.PopularMangaRepositoryImpl
import io.silv.manga.domain.repositorys.RecentMangaRepository
import io.silv.manga.domain.repositorys.RecentMangaRepositoryImpl
import io.silv.manga.domain.repositorys.SavedMangaRepository
import io.silv.manga.domain.repositorys.SavedMangaRepositoryImpl
import io.silv.manga.domain.repositorys.SearchMangaRepository
import io.silv.manga.domain.repositorys.SearchMangaRepositoryImpl
import io.silv.manga.domain.repositorys.SeasonalMangaRepository
import io.silv.manga.domain.repositorys.SeasonalMangaRepositoryImpl
import io.silv.manga.domain.repositorys.chapter.ChapterListRepository
import io.silv.manga.domain.repositorys.chapter.ChapterEntityRepository
import io.silv.manga.domain.repositorys.chapter.ChapterListRepositoryImpl
import io.silv.manga.domain.repositorys.chapter.OfflineFirstChapterInfoRepository
import io.silv.manga.domain.repositorys.people.ArtistListRepository
import io.silv.manga.domain.repositorys.people.ArtistListRepositoryImpl
import io.silv.manga.domain.repositorys.people.AuthorListRepository
import io.silv.manga.domain.repositorys.people.AuthorListRepositoryImpl
import io.silv.manga.domain.repositorys.tags.TagRepository
import io.silv.manga.domain.repositorys.tags.TagRepositoryImpl
import io.silv.manga.domain.usecase.useCaseModule
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val repositoryModule = module {

    includes(useCaseModule)

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

    singleOf(::ArtistListRepositoryImpl) withOptions {
        bind<ArtistListRepository>()
    }

    singleOf(::ChapterListRepositoryImpl) withOptions {
        bind<ChapterListRepository>()
    }
}