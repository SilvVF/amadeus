@file:Suppress("unused")

package io.silv.common.model

import androidx.compose.runtime.Stable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

enum class Result {
    ok,
    error,
}

enum class State {
    draft,
    submitted,
    published,
    rejected,
}

enum class Group {
    content,
    format,
    genre,
    theme,
}

enum class TagsMode(
    val string: String,
) {
    OR("OR"),
    AND("AND"),
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
    asc,
    desc,
}

enum class ContentRating {
    safe,
    suggestive,
    erotica,
    pornographic,
}

enum class Status {
    completed,
    ongoing,
    cancelled,
    hiatus,
}

enum class Related {
    monochrome,
    main_story,
    adapted_from,
    based_on,
    prequel,
    side_story,
    doujinshi,
    same_franchise,
    shared_universe,
    sequel,
    spin_off,
    alternate_story,
    alternate_version,
    preserialization,
    colored,
    serialization,
}

enum class PublicationDemographic {
    shounen,
    shoujo,
    josei,
    seinen,
}

enum class CoverIncludesFilter {
    manga,
    user,
}

enum class UpdateType {
    Chapter,
    Volume,
    Other,
}

enum class Season {
    Winter,
    Spring,
    Summer,
    Fall,
}

enum class TimePeriod {
    OneYear,
    SixMonths,
    ThreeMonths,
    LastMonth,
    OneWeek,
    AllTime,
}

enum class ReaderOrientation {
    Vertical,
    Horizontal,
}

enum class ReaderDirection {
    Ltr,
    Rtl,
}

@Stable
enum class AppTheme {
    DYNAMIC_COLOR_DARK{
        override fun toString(): String {
            return "Dynamic (dark)"
        }
    },
    DYNAMIC_COLOR_LIGHT{
        override fun toString(): String {
            return "Dynamic (light)"
        }
    },
    DYNAMIC_COLOR_DEFAULT{
        override fun toString(): String {
            return "Dynamic (system default))"
        }
    },
    SYSTEM_DEFAULT {
        override fun toString(): String {
            return "System default"
        }
    },
    DARK {
        override fun toString(): String {
            return "Dark"
        }
    },
    LIGHT {
        override fun toString(): String {
            return "Light"
        }
    }
}

@Stable
enum class AutomaticUpdatePeriod(val duration: Duration) {
    Off(Duration.ZERO) { override fun toString(): String { return "Off" }},
    H12(12.hours){ override fun toString(): String { return "Every 12 hours" }},
    H24(24.hours){ override fun toString(): String { return "Daily" }},
    H48(48.hours){ override fun toString(): String { return "Every 2 days" }},
    H72(72.hours){ override fun toString(): String { return "Every 3 days" }},
    W1(7.days){ override fun toString(): String { return "Weekly" }}
}