package io.silv.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.silv.common.DependencyAccessor

@Composable
@OptIn(DependencyAccessor::class)
fun <T> rememberDataDependency(get: DataDependencies.() -> T) = remember { dataDeps.get() }
