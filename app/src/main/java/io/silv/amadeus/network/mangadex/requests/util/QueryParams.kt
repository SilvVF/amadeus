package io.silv.amadeus.network.mangadex.requests.util

import kotlin.reflect.full.memberProperties

/**
 * Interface that marks a class as representing query parameters for a request.
 * This gives the class access to [createQueryParams] which uses the member names
 * and values to provide a list of [QueryParam].
 */
interface QueryParams

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