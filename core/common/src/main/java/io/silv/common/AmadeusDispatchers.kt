package io.silv.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface AmadeusDispatchers {
    val io: CoroutineDispatcher

    val main: CoroutineDispatcher

    val default: CoroutineDispatcher

    val unconfined: CoroutineDispatcher

    companion object {
        val default =
            object : AmadeusDispatchers {
                override val io: CoroutineDispatcher
                    get() = Dispatchers.IO
                override val main: CoroutineDispatcher
                    get() = Dispatchers.Main
                override val default: CoroutineDispatcher
                    get() = Dispatchers.Default
                override val unconfined: CoroutineDispatcher
                    get() = Dispatchers.Unconfined
            }
    }
}
