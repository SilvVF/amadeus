package io.silv.ui

import cafe.adriel.voyager.navigator.Navigator

interface ReselectTab : cafe.adriel.voyager.navigator.tab.Tab {
    suspend fun onReselect(navigator: Navigator) {}
}
