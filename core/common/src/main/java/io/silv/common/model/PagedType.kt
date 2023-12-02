package io.silv.common.model

sealed class PagedType {
    data object Popular: PagedType()
    data object Latest: PagedType()
    data class Query(
        val filters: QueryFilters
    ): PagedType()
    data class Period(
        val tagId: String,
        val timePeriod: TimePeriod = TimePeriod.AllTime
    ): PagedType()
}

data class QueryFilters(
    val query: String = "",
    val tagId: String? = null ,
    val timePeriod: TimePeriod = TimePeriod.AllTime
)