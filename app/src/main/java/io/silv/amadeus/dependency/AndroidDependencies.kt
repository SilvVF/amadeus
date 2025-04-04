package io.silv.amadeus.dependency

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import io.silv.common.DependencyAccessor

/**
 * Global var for making the [AndroidDependencies] accessible.
 */
@DependencyAccessor
public lateinit var androidDeps: AndroidDependencies

@OptIn(DependencyAccessor::class)
public val LifecycleOwner.commonDepsLifecycle: AndroidDependencies
    get() = androidDeps

@OptIn(DependencyAccessor::class)
abstract class AndroidDependencies {

    abstract val application: Application
}