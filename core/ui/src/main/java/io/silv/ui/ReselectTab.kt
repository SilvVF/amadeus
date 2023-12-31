package io.silv.ui

import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabNavigator

interface ReselectTab : cafe.adriel.voyager.navigator.tab.Tab {
    suspend fun onReselect(navigator: Navigator) {}
}

interface GlobalSearchTab : cafe.adriel.voyager.navigator.tab.Tab {
    suspend fun onSearch(query: String?,  navigator: TabNavigator) {}
}
