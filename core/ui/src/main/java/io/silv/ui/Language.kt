package io.silv.ui

import androidx.annotation.DrawableRes

enum class Language(
    val string: String,
    val code: String,
    @DrawableRes val resId: Int
) {
    Arabic("Arabic","sa",R.drawable.sa),
    Bengali("Bengali","bd", R.drawable.bd),
    Bulgarian("Bulgarian","bg", R.drawable.bg),
    Burmese("Burmese","mm", R.drawable.mm),
    ChineseSimp("Chinese (Simplified)","cn", R.drawable.cn),
    ChineseTrad("Chinese (Traditional)","hk", R.drawable.hk),
    Czech("Czech","cz", R.drawable.cz),
    Danish("Danish","dk", R.drawable.dk),
    Dutch("Dutch","nl", R.drawable.nl),
    English("English","en", R.drawable.gb),
    Filipino("Filipino","ph", R.drawable.ph),
    Finnish("Finnish","fi", R.drawable.fi),
    French("French","fr", R.drawable.fr),
    German("German","de", R.drawable.de),
    Greek("Greek","gr", R.drawable.gr),
    Hebrew("Hebrew","il", R.drawable.il),
    Hindi("Hindi","in",  R.drawable.`in`),
    Hungarian("Hungarian","hu",  R.drawable.hu),
    Indonesian("Indonesian","id",  R.drawable.id),
    Italian("Italian","it", R.drawable.it),
    Japanese("Japanese","jp", R.drawable.jp),
    Korean("Korean","kr",  R.drawable.kr),
    Lithuanian("Lithuanian","lt",  R.drawable.lt),
    Malay("Malay","my",  R.drawable.my),
    Mongolian("Mongolian","mn", R.drawable.mn),
    Polish("Polish","pl", R.drawable.pl),
    PortugueseBr("Portuguese (Br)","br", R.drawable.br),
    PortuguesePt("Portuguese (Pt)","pt",  R.drawable.pt),
    Romanian("Romanian","ro",  R.drawable.ro),
    Russian("Russian","ru", R.drawable.ru),
    Serbian("Serbian","rs", R.drawable.rs),
    SpanishEs("Spanish (Es)","es", R.drawable.es),
    SpanishLATAM("Spanish (LATAM)","mx", R.drawable.mx),
    Swedish("Swedish","se", R.drawable.se),
    Thai("Thai","th", R.drawable.th),
    Turkish("Turkish","tr", R.drawable.tr),
    Ukrainian("Ukrainian","ua", R.drawable.ua),
    Vietnamese("Vietnamese","vn", R.drawable.vn),
}