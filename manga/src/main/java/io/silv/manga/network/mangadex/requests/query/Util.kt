package io.silv.manga.network.mangadex.requests.query

import android.util.Log
import kotlin.reflect.full.memberProperties

/**
 * Finds the member value based on the fieldName.
 * @return member value or null if cast failed / not found
 */
@Throws(IllegalAccessException::class, ClassCastException::class)
private inline fun <reified T> Any.getField(fieldName: String): T? {
    this::class.memberProperties.forEach { kCallable ->
        if (fieldName == kCallable.name) {
            return kCallable.getter.call(this) as T?
        }
    }
    return null
}

/**
 * Creates the query parameters from class member using reflection
 */
fun QueryParams.createQueryParams(): List<QueryParam> {
    val instance = this
    return buildList {
        instance::class.members.forEach {  member ->
            val fieldValue = instance.getField<Any?>(member.name) ?: return@forEach
            add(
                QueryParam(
                    member.name,
                    fieldValue.toString()
                )
            )
        }
    }
}

/**
 * appends each part of the list to the [StringBuilder] as name[]=value.
 * each value past the first in the list is added on as &name[]=value.
 * the & is appended after only if it is not the last element.
 */
private fun StringBuilder.appendList(name: String, value: String) {
    val list = value.drop(1).dropLast(1).split(',', ignoreCase = true)
    list.forEachIndexed { i, it ->
        append(name)
        append("[]=")
        append(it)
        if (i != list.lastIndex)
            append('&')
    }
}

/**
 * appends each part of the map to the [StringBuilder] as name[[attribute]]=attributeValue.
 * each value past the first in the list is added on as &name[[attribute]]=attributeValue.
 * the & is appended after only if it is not the last element.
 * - ex order[[createdAt]]=desc
 */
private fun StringBuilder.appendObject(name: String, value: String) {
    val list = value.drop(1).dropLast(1)
        .split('=', ignoreCase = true)
        .chunked(2)
    list.forEachIndexed { i, (attribute, order) ->
        append(name)
        append('[')
        append(attribute)
        append(']')
        append('=')
        append(order)
        if (i != list.lastIndex) {
            append('&')
        }
    }
}

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
                when (value.firstOrNull()) {
                    '{' ->  appendObject(name, value)
                    '[' -> appendList(name, value)
                    else -> {
                        append(name)
                        append('=')
                        append(value)
                    }
                }
                if (index != params.lastIndex) {
                    append('&')
                }
            }
        }
    }
        .toString().also {
            Log.d("QUERY", it)
        }
}