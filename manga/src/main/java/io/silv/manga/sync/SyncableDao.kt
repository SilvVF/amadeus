package io.silv.manga.sync

import io.silv.manga.local.entity.AmadeusEntity

internal interface SyncableDao<in E: AmadeusEntity<Any?>>

