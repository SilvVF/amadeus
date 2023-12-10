@file:Suppress("unused")

package io.silv.common.model

enum class Result {
    ok, error
}

enum class State {
    draft, submitted, published, rejected
}

enum class Group {
    content, format, genre, theme
}

enum class TagsMode(
    val string: String
) {
    OR("OR"), AND("AND")
}

enum class Order {
    createdAt,
    updatedAt,
    publishAt,
    readableAt,
    volume,
    chapter,
}

enum class OrderBy {
    asc,  desc
}

enum class ContentRating {
    safe, suggestive, erotica, pornographic
}

enum class Status {
    completed, ongoing, cancelled, hiatus
}

enum class Related {
    monochrome, main_story, adapted_from, based_on, prequel, side_story, doujinshi, same_franchise, shared_universe, sequel, spin_off, alternate_story, alternate_version, preserialization, colored, serialization
}

enum class PublicationDemographic {
    shounen, shoujo, josei, seinen
}

enum class CoverIncludesFilter {
    manga, user
}

enum class UpdateType {
    Chapter, Volume, Other
}

enum class Season{
    Winter, Spring, Summer, Fall
}

enum class TimePeriod {
    SixMonths, ThreeMonths, LastMonth, OneWeek, AllTime
}

enum class ReaderOrientation {
    Vertical, Horizontal
}

enum class ReaderDirection {
    Ltr, Rtl
}