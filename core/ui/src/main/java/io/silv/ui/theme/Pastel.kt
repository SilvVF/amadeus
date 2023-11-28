package io.silv.ui.theme

import io.silv.ui.R

private object LightColors {

    val list: List<Int> = listOf(
        R.color.green,
        R.color.pink,
        R.color.orange,
        R.color.orange_alternate,
        R.color.blue,
        R.color.blue_mid,
        R.color.brown,
        R.color.green_mid,
        R.color.pink_high
    )
}

private object DarkColors {

    val list: List<Int> = listOf(
        R.color.green_dark,
        R.color.black_dark,
        R.color.blue_dark,
        R.color.blue_high_dark,
        R.color.blue_mid_dark,
        R.color.brown_dark,
        R.color.orange_dark,
        R.color.pink_dark,
        R.color.purple_dark
    )
}

object Pastel {

    fun getColorLight(): Int{
        return LightColors.list.random()
    }

    fun getColorDark(): Int{
        return DarkColors.list.random()
    }
}