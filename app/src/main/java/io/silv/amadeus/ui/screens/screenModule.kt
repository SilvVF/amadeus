package io.silv.amadeus.ui.screens

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val screenModule = module {

    factoryOf(::HomeSM)
}