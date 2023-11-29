package io.silv.navigation

import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.navigator.Navigator

infix fun Navigator.push(item: SharedScreen) {
    push(
        ScreenRegistry.get(item)
    )
}

infix fun Navigator.push(items: List<SharedScreen>) {
    push(
        items.map { item ->
            ScreenRegistry.get(item)
        }
    )
}

infix fun Navigator.replace(item: SharedScreen) {
    replace(
        ScreenRegistry.get(item)
    )
}

operator fun Navigator.plusAssign(item: SharedScreen) {
    plusAssign(
        ScreenRegistry.get(item)
    )
}

operator fun Navigator.plusAssign(items: List<SharedScreen>) {
    plusAssign(
        items.map { item ->
            ScreenRegistry.get(item)
        }
    )
}

infix fun Navigator.replaceAll(item: SharedScreen) {
    replaceAll(
        ScreenRegistry.get(item)
    )
}
infix fun Navigator.replaceAll(items: List<SharedScreen>) {
    replaceAll(
        items.map { item ->
            ScreenRegistry.get(item)
        }
    )
}







