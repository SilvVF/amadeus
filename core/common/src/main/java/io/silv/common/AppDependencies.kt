package io.silv.common

import kotlinx.coroutines.CoroutineScope

@OptIn(DependencyAccessor::class)
public lateinit var appDeps: AppDependencies

@OptIn(DependencyAccessor::class)
abstract class AppDependencies {

    val dispatchers: AmadeusDispatchers = AmadeusDispatchers.default

    val applicationScope: CoroutineScope = ApplicationScope()
}