package io.silv.amadeus.dependency

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import io.silv.common.DependencyAccessor

/**
 * Global var for making the [CommonDependencies] accessible.
 */
@DependencyAccessor
public lateinit var commonDeps: CommonDependencies

@OptIn(DependencyAccessor::class)
public val LifecycleOwner.commonDepsLifecycle: CommonDependencies
    get() = commonDeps

@OptIn(DependencyAccessor::class)
abstract class CommonDependencies {

    abstract val application: Application
}