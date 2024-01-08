package io.silv.data.manga

import com.skydoves.sandwich.getOrThrow
import io.silv.data.download.CoverCache
import io.silv.database.dao.UserListDao
import io.silv.database.entity.list.UserListEntity
import io.silv.datastore.MangaDexUserStore
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.model.Manga
import io.silv.domain.manga.repository.MangaRepository
import io.silv.network.MangaDexApi
import io.silv.network.model.list.Data
import io.silv.network.requests.MangaRequest
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GetUserLists(
    private val mangaDexApi: MangaDexApi,
    private val mangaRepository: MangaRepository,
    private val userListDao: UserListDao,
    private val getManga: GetManga,
    private val coverCache: CoverCache,
    private val store: MangaDexUserStore
) {

    fun subscribe(scope: CoroutineScope) =
        store.observeUserIds().map { userIds ->

            updateUserListsIfNeeded(userIds)

            userListDao.observeUserListsWithManga()
                .map {
                    it.toList().map { (list, manga )->
                        UserList(
                            listId = list.listId,
                            createdBy = list.createdBy,
                            version = list.version,
                            mangas = manga.map(MangaMapper::mapManga).toImmutableList(),
                            name = list.name,
                            id = list.id
                        )
                    }
                }
                .catch { it.printStackTrace() }
                .stateIn(
                    scope = scope,
                    SharingStarted.Lazily,
                    emptyList()
                )
        }
            .catch { it.printStackTrace() }

    private suspend fun updateUserListsIfNeeded(userIds: List<String>) = runCatching {
        userIds.forEach { id ->

            mangaDexApi.getUserLists(id = id)
                .getOrThrow()
                .data
                .updateUserListAndGetManga(createdBy = id)
        }
    }

    private suspend fun List<Data>.updateUserListAndGetManga(
        createdBy: String
    ) = forEach { list ->

        val prevList = userListDao.getUserListByListId(list.id)

        if (prevList != null && prevList.version == list.attributes.version)
            return

        val userList = UserListEntity(
            listId = list.id,
            createdBy = createdBy,
            version = list.attributes.version,
            name = list.attributes.name,
            mangaIds = list.relationships
                .filter { it.type == "manga" }
                .map { it.id }
        )

        val mangas = mangaDexApi.getMangaList(
            mangaRequest = MangaRequest(
                limit = 100,
                ids = userList.mangaIds,
                includes = listOf("cover_art", "author", "artist")
            )
        )
            .getOrThrow()
            .data

        mangaRepository.upsertManga(
            mangas.map(MangaMapper::dtoToUpdate)
        )
        userListDao.upsertUserList(userList)
    }
}

data class UserList(
    val listId: String,
    val createdBy: String,
    val version: Int,
    val mangas: ImmutableList<Manga>,
    val name: String,
    val id: Long
)