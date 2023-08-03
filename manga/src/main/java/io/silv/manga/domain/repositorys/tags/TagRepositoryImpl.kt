package io.silv.manga.domain.repositorys.tags

import io.silv.ktor_response_mapper.getOrThrow
import io.silv.manga.local.dao.TagDao
import io.silv.manga.local.entity.TagEntity
import io.silv.manga.network.mangadex.MangaDexApi
import io.silv.manga.sync.Synchronizer
import io.silv.manga.sync.syncVersions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

internal class TagRepositoryImpl(
    private val  tagDao: TagDao,
    private val mangaDexApi: MangaDexApi
) : TagRepository {


    override fun allTags(): Flow<List<TagEntity>> {
        return tagDao.getAllTags()
    }

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean {
        val tags = tagDao.getAllTags().first()
        return synchronizer.syncVersions(
            getLocalWithVersions =  { tags.map { it.version to it } },
            getNetworkWithVersion = {
                mangaDexApi.getTagList()
                    .getOrThrow()
                    .data.map {
                        it.attributes.version to it
                    }
            },
            networkToKey = { tagData -> tagData.id },
            lastUpdatedEpochSeconds = { tags.minByOrNull { it.lastUpdatedEpochSeconds }?.lastUpdatedEpochSeconds ?: 0L },
            delete = { entity ->
                tagDao.deleteTag(entity)
            },
            update = { network, local ->
                tagDao.updateTag(
                    local.copy(
                        group = network.attributes.group,
                        version =  network.attributes.version,
                        name = network.attributes.name["en"] ?: network.attributes.name.values.first(),
                        lastUpdatedEpochSeconds = Clock.System.now().epochSeconds
                    )
                )
            },
            insert = { network ->
                tagDao.upsertTag(
                    TagEntity(
                        id = network.id,
                        group = network.attributes.group,
                        version = network.attributes.version,
                        name = network.attributes.name["en"]
                            ?: network.attributes.name.values.first(),
                        lastUpdatedEpochSeconds = Clock.System.now().epochSeconds
                    )
                )
            }
        )
    }
}