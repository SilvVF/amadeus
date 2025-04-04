package io.silv.common

import kotlinx.coroutines.CoroutineScope

@OptIn(DependencyAccessor::class)
public lateinit var commonDeps: CommonDependencies

@OptIn(DependencyAccessor::class)
abstract class CommonDependencies {

    val dispatchers: AmadeusDispatchers = AmadeusDispatchers.default

    val applicationScope: CoroutineScope = ApplicationScope()
}