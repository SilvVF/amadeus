package io.silv.common

import kotlinx.coroutines.flow.Flow

interface NetworkConnectivity {
    val online: Flow<Boolean>
}
