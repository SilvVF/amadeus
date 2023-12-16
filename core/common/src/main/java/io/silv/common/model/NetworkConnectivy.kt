package io.silv.common.model

import kotlinx.coroutines.flow.Flow

interface NetworkConnectivity {
    val online: Flow<Boolean>
}