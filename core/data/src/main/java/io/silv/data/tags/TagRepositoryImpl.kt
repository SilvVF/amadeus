package io.silv.data.tags

import io.silv.common.time.epochSeconds
import io.silv.data.util.syncVersions
import io.silv.database.dao.TagDao
import io.silv.database.entity.list.TagEntity
import io.silv.ktor_response_mapper.getOrThrow
import io.silv.network.MangaDexApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class TagRepositoryImpl(
    private val  tagDao: TagDao,
    private val mangaDexApi: MangaDexApi
) : TagRepository {


    override fun allTags(): Flow<List<TagEntity>> {
        return tagDao.getAllTags()
    }

    override suspend fun sync(): Boolean {
        val tags = tagDao.getAllTags().first()
        return syncVersions(
            getLocalWithVersions =  { tags.map { it.version to it } },
            getNetworkWithVersion = {
                mangaDexApi.getTagList()
                    .getOrThrow()
                    .data
                    .map {
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
                        lastUpdatedEpochSeconds = epochSeconds()
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
                        lastUpdatedEpochSeconds = epochSeconds()
                    )
                )
            }
        )
    }
}