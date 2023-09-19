package io.silv.manga.network.mangadex.requests.query

/**
 * Interface that marks a class as representing query parameters for a request.
 * This gives the class access to [createQueryParams] which uses the member names
 * and values to provide a list of [QueryParam].
 */
interface QueryParams {
    fun createQueryParams(): List<QueryParam>
}
