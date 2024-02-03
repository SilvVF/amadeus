package io.silv.network.query

/**
 * @param name key for a query parameter
 * @param value value for a query parameter
 *
 * call [createQuery] on a [List] of [QueryParam] and give the base url to create a
 * request with query parameters.
 */
data class QueryParam(
    val name: String,
    val value: String,
)
