@file:Suppress("unused")

package io.silv.ktor_response_mapper.client

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.silv.ktor_response_mapper.ApiResponse

suspend inline fun <reified T> KSandwichClient.get(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): ApiResponse<T> {
    return ApiResponse.of { client.get(urlString, block) }
}

suspend inline fun <reified T> KSandwichClient.post(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): ApiResponse<T> {
    return ApiResponse.of { client.post(urlString, block) }
}

suspend inline fun <reified T> KSandwichClient.put(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): ApiResponse<T> {
    return ApiResponse.of { client.put(urlString, block) }
}

suspend inline fun <reified T> KSandwichClient.patch(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): ApiResponse<T> {
    return ApiResponse.of { client.patch(urlString, block) }
}