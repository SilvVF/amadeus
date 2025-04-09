package io.silv.common

import kotlinx.coroutines.CoroutineScope

@DependencyAccessor
public lateinit var commonDeps: CommonDependencies

@OptIn(DependencyAccessor::class)
abstract class CommonDependencies {

    val dispatchers: AmadeusDispatchers = AmadeusDispatchers.default

    val applicationScope: CoroutineScope = ApplicationScope()
}