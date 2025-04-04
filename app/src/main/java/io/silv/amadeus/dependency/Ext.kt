package io.silv.amadeus.dependency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.silv.common.CommonDependencies
import io.silv.common.DependencyAccessor
import io.silv.common.commonDeps
import io.silv.di.DataDependencies
import io.silv.di.dataDeps

/**
 * helper to get a dep from compose without having to annotate
 * still findable with search for [rememberDependency]
 */
@Composable
@OptIn(DependencyAccessor::class)
fun <T> rememberCommonDependency(get: CommonDependencies.() -> T) = remember { commonDeps.get() }

@Composable
@OptIn(DependencyAccessor::class)
fun <T> rememberAndroidDependency(get: AndroidDependencies.() -> T) = remember { androidDeps.get() }


@Composable
@OptIn(DependencyAccessor::class)
fun <T> rememberDataDependency(get: DataDependencies.() -> T) = remember { dataDeps.get() }