package io.silv.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

data class ApplicationScope @OptIn(DelicateCoroutinesApi::class) constructor(
    val scope: CoroutineScope = GlobalScope
): CoroutineScope by scope
