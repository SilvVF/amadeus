package io.silv.explore

import android.util.Log
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.model.ContentRating
import io.silv.common.model.Order
import io.silv.common.model.OrderBy
import io.silv.common.model.PublicationDemographic
import io.silv.common.model.QueryFilters
import io.silv.common.model.Status
import io.silv.common.model.TagsMode
import io.silv.di.dataDeps
import io.silv.domain.TagRepository
import io.silv.model.DomainTag
import io.silv.ui.Language
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

class FilterScreenViewModel @OptIn(DependencyAccessor::class) constructor(
    tagRepository: TagRepository = dataDeps.tagRepository,
) : ScreenModel {
    private val mutableState = MutableStateFlow(FilterState())
    val state = mutableState.asStateFlow()

    init {
        tagRepository.allTags().onEach { tags ->
            mutableState.update { state ->
                state.copy(
                    categoryToTags =
                    tags
                        .groupBy { tag -> tag.group }
                        .mapValues { (_, tags) ->
                            tags.toImmutableList()
                        }
                        .toImmutableMap(),
                )
            }
        }
            .launchIn(screenModelScope)
    }

    private fun <T> List<T>.toggleItem(item: T): ImmutableList<T> {
        return (
            if (contains(item)) this - item else this + item
            )
            .toImmutableList()
    }

    private fun <T> List<T>.toggleItems(items: List<T>): ImmutableList<T> {
        return (
            if (containsAll(items)) this - items.toSet() else this + items
            )
            .toImmutableList()
    }

    private fun updateFilters(block: (filters: UiQueryFilters) -> UiQueryFilters) {
        mutableState.update {
            it.copy(
                queryFilters = block(it.queryFilters),
            )
        }
    }

    fun resetFilter() {
        screenModelScope.launch {
            mutableState.emit(FilterState())
        }
    }

    fun updateFilter(action: FilterAction) {
        Log.d("Filter", "Updating $action")
        screenModelScope.launch {
            when (action) {
                is FilterAction.ChangeArtist ->
                    updateFilters {
                        it.copy(
                            artists = it.artists.toggleItem(action.artist),
                        )
                    }
                is FilterAction.ChangeAuthor ->
                    updateFilters {
                        it.copy(
                            artists = it.authors.toggleItem(action.author),
                        )
                    }
                is FilterAction.ChangeContentRating ->
                    updateFilters {
                        it.copy(
                            contentRating = it.contentRating.toggleItem(action.rating),
                        )
                    }
                is FilterAction.ChangeCreatedAt ->
                    updateFilters {
                        it.copy(
                            createdAtSince = action.time,
                        )
                    }
                is FilterAction.ChangeGroup ->
                    updateFilters {
                        it.copy(
                            group = action.group,
                        )
                    }
                is FilterAction.ChangeIds ->
                    updateFilters {
                        it.copy(
                            ids = it.ids.toggleItem(action.id),
                        )
                    }
                is FilterAction.ChangeIncludes ->
                    updateFilters {
                        it.copy(
                            includes = it.includes.toggleItem(action.includes),
                        )
                    }
                is FilterAction.ChangeOrder ->
                    updateFilters {
                        it.copy(
                            order = action.order,
                        )
                    }
                is FilterAction.ChangeOrderBy ->
                    updateFilters {
                        it.copy(
                            orderBy = action.order,
                        )
                    }
                is FilterAction.ChangePublicationDemographic ->
                    updateFilters {
                        it.copy(
                            publicationDemographic = it.publicationDemographic.toggleItem(
                                action.demographic
                            ),
                        )
                    }
                is FilterAction.ChangeStatus ->
                    updateFilters {
                        it.copy(
                            status = it.status.toggleItem(action.status),
                        )
                    }
                is FilterAction.ChangeTranslatedLanguage ->
                    updateFilters {
                        it.copy(
                            availableTranslatedLanguage = it.availableTranslatedLanguage.toggleItem(
                                action.language
                            ),
                        )
                    }
                is FilterAction.ChangeUpdatedAt ->
                    updateFilters {
                        it.copy(updatedAtSince = it.updatedAtSince)
                    }
                is FilterAction.ChangeYear ->
                    updateFilters {
                        it.copy(year = it.year)
                    }
                is FilterAction.ExcludeTag ->
                    updateFilters {
                        it.copy(excludedTags = it.excludedTags.toggleItem(action.tag))
                    }
                is FilterAction.IncludeTag ->
                    updateFilters {
                        it.copy(includedTags = it.includedTags.toggleItem(action.tag))
                    }
                FilterAction.ToggleExcludeTagMode ->
                    updateFilters {
                        it.copy(
                            excludedTagsMode =
                            when (it.excludedTagsMode) {
                                TagsMode.OR -> TagsMode.AND
                                TagsMode.AND -> TagsMode.OR
                            },
                        )
                    }
                FilterAction.ToggleHasAvailableChapters ->
                    updateFilters {
                        it.copy(hasAvailableChapters = !it.hasAvailableChapters)
                    }
                FilterAction.ToggleIncludeTagMode ->
                    updateFilters {
                        it.copy(
                            includedTagsMode =
                            when (it.includedTagsMode) {
                                TagsMode.OR -> TagsMode.AND
                                TagsMode.AND -> TagsMode.OR
                            },
                        )
                    }
                is FilterAction.MangaType ->
                    updateFilters {
                        it.copy(
                            originalLanguage = it.originalLanguage.toggleItems(action.languages),
                        )
                    }
            }
        }
    }
}

sealed interface FilterAction {
    data object ToggleHasAvailableChapters : FilterAction

    data class ChangeAuthor(val author: String) : FilterAction

    data class ChangeArtist(val artist: String) : FilterAction

    data class ChangeYear(val year: String) : FilterAction

    data class IncludeTag(val tag: String) : FilterAction

    data class ExcludeTag(val tag: String) : FilterAction

    data object ToggleIncludeTagMode : FilterAction

    data object ToggleExcludeTagMode : FilterAction

    data class ChangeStatus(val status: Status) : FilterAction

    data class MangaType(val languages: List<Language>) : FilterAction

    data class ChangeTranslatedLanguage(val language: Language) : FilterAction

    data class ChangePublicationDemographic(val demographic: PublicationDemographic) : FilterAction

    data class ChangeIds(val id: String) : FilterAction

    data class ChangeContentRating(val rating: ContentRating) : FilterAction

    data class ChangeCreatedAt(val time: LocalDateTime?) : FilterAction

    data class ChangeUpdatedAt(val time: LocalDateTime?) : FilterAction

    data class ChangeOrder(val order: Order?) : FilterAction

    data class ChangeOrderBy(val order: OrderBy) : FilterAction

    data class ChangeIncludes(val includes: String) : FilterAction

    data class ChangeGroup(val group: String) : FilterAction
}

@Stable
data class FilterState(
    val categoryToTags: ImmutableMap<String, ImmutableList<DomainTag>> = persistentMapOf(),
    val queryFilters: UiQueryFilters = UiQueryFilters(),
)

@Stable
data class UiQueryFilters(
    val title: String = "",
    val authors: ImmutableList<String> = persistentListOf(),
    val artists: ImmutableList<String> = persistentListOf(),
    val year: String = "",
    val includedTags: ImmutableList<String> = persistentListOf(),
    val includedTagsMode: TagsMode = TagsMode.OR,
    val excludedTags: ImmutableList<String> = persistentListOf(),
    val excludedTagsMode: TagsMode = TagsMode.OR,
    val status: ImmutableList<Status> = persistentListOf(),
    val originalLanguage: ImmutableList<Language> = persistentListOf(),
    val availableTranslatedLanguage: ImmutableList<Language> = persistentListOf(),
    val publicationDemographic: ImmutableList<PublicationDemographic> = persistentListOf(),
    val ids: ImmutableList<String> = persistentListOf(),
    val contentRating: ImmutableList<ContentRating> = persistentListOf(
        ContentRating.safe,
        ContentRating.suggestive
    ),
    var createdAtSince: LocalDateTime? = null,
    val updatedAtSince: LocalDateTime? = null,
    val order: Order? = null,
    val orderBy: OrderBy? = null,
    val includes: ImmutableList<String> = persistentListOf(),
    val hasAvailableChapters: Boolean = true,
    val group: String = "",
)

fun UiQueryFilters.toQueryFilters(): QueryFilters {
    return QueryFilters(
        title = title.ifBlank { null },
        authorOrArtist = null,
        authors = authors.ifEmpty { null },
        artists = artists.ifEmpty { null },
        year = year.ifBlank { null }?.toIntOrNull(),
        includedTags = includedTags.ifEmpty { null },
        includedTagsMode = includedTagsMode,
        excludedTags = excludedTags.ifEmpty { null },
        excludedTagsMode = excludedTagsMode,
        status = status.map { it.toString() }.ifEmpty { null },
        originalLanguage = originalLanguage.map { it.toString() }.ifEmpty { null },
        availableTranslatedLanguage = availableTranslatedLanguage.map { it.toString() }.ifEmpty { null },
        publicationDemographic = publicationDemographic.map { it.toString() }.ifEmpty { null },
        ids = ids.ifEmpty { null },
        contentRating = contentRating.map { it.toString() }.ifEmpty { null },
        createdAtSince = createdAtSince,
        updatedAtSince = updatedAtSince,
        order = takeIf { orderBy != null && order != null }?.let {
            mapOf(
                order.toString() to orderBy.toString()
            )
        },
        includes = includes.ifEmpty { null },
        hasAvailableChapters = hasAvailableChapters,
        group = group.ifBlank { null },
    )
}
