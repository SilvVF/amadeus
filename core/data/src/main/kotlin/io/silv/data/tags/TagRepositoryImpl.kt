package io.silv.data.tags

import com.skydoves.sandwich.getOrThrow
import io.silv.common.time.epochSeconds
import io.silv.data.util.syncVersions
import io.silv.database.dao.TagDao
import io.silv.database.entity.list.TagEntity
import io.silv.data.TagRepository
import io.silv.model.DomainTag
import io.silv.network.MangaDexApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class TagRepositoryImpl(
    private val  tagDao: TagDao,
    private val mangaDexApi: MangaDexApi
) : TagRepository {


    override fun allTags(): Flow<List<DomainTag>> {
        return tagDao.getAllTags().map { list ->
            list.map {
                DomainTag(group = it.group, name = it.name, id = it.id)
            }
        }
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