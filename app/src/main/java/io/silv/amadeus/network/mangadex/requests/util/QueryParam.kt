package io.silv.amadeus.network.mangadex.requests.util

import kotlin.text.StringBuilder

/**
 * @param name key for a query parameter
 * @param value value for a query parameter
 */
data class QueryParam(
    val name: String,
    val value: String
)

/**
 * Takes a base url and outputs a new url with the query parameters attached.
 * Uses values from [QueryParam] list.
 * This attaches onto the base url from ?.
 * This will also strip any trailing / from the base string.
 *
 * - ex. base = https://google.com   this = [QueryParam(name=test, value=test)]
 * returns https://google.com?test=test
 *
 * - ex. base = https://google.com/   this = [QueryParam(name=test, value=test)]
 * returns https://google.com?test=test
 *
 * - ex. base = https://google.com
 *  this = [QueryParam(name=test, value=test), QueryParam(name=test1, value=test1)]
 * returns https://google.com?test=test&test1=test1
 */
fun List<QueryParam>.createQuery(base: String): String {
    val params = this
    return StringBuilder().apply {
        append(
            base.removeSuffix("/")
        )
        if (params.isNotEmpty()) {
            append('?')
            params.forEachIndexed { index, (name, value) ->
                if (value.startsWith("[")) {
                    val list = value.drop(1).dropLast(1).split(',', ignoreCase = true)
                    list.forEachIndexed { i, it ->
                        append(name)
                        append("[]=")
                        append(it)
                        if (i != list.lastIndex)
                            append('&')
                    }
                } else {
                    append(name)
                    append('=')
                    append(value)
                }
                if (index != params.lastIndex) {
                    append('&')
                }
            }
        }
    }
        .toString()
}

