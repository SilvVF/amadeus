@file:Suppress("unused", "RedundantVisibilityModifier")

package io.silv.ktor_response_mapper

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.silv.ktor_response_mapper.operators.ApiResponseOperator
import io.silv.ktor_response_mapper.operators.ApiResponseSuspendOperator
import kotlinx.coroutines.launch


sealed class ApiResponse<out T> {

    data class Success<T>(
        private val response: HttpResponse,
        val data: T
    ): ApiResponse<T>() {
        val statusCode = getStatusCodeFromResponse(response)
        val headers: Headers = response.headers
        val raw: HttpResponse = response
        override fun toString(): String = "[ApiResponse.Success](data=$data)"
    }

    sealed class Failure<T> : ApiResponse<T>() {

        data class Error<T>(
            private val response: HttpResponse,
            val errorBody: String
        ) : Failure<T>() {
            val statusCode: StatusCode = getStatusCodeFromResponse(response)
            val headers: Headers = response.headers
            val raw: HttpResponse = response
            override fun toString(): String {
                val errorBody = errorBody
                return errorBody.ifEmpty {
                    "[ApiResponse.Failure.Error-$statusCode](errorResponse=$response)"
                }
            }
        }

        data class Exception<T>(val exception: Throwable) : Failure<T>() {
            val message: String? = exception.localizedMessage
            override fun toString(): String = "[ApiResponse.Failure.Exception](message=$message)"
        }
    }

    public companion object {
        @JvmSynthetic
        suspend inline fun <reified T> of(
            successCodeRange: IntRange = 200..299 ,
            data: T? = null,
            crossinline f: suspend () -> HttpResponse,
        ): ApiResponse<T> = try {
            val response = f()
            if (response.status.value in successCodeRange) {
                Success(response, data ?: response.body<T>())
            } else {
                Failure.Error(response, response.body())
            }
        } catch (ex: Exception) {
            Failure.Exception(ex)
        }.operate()

        /**
         * @author skydoves (Jaewoong Eum)
         *
         * Operates if there is a global [io.silv.ktor_response_mapper.operators.SandwichOperator]
         * which operates on [ApiResponse]s globally on each response and returns the target [ApiResponse].
         *
         * @return [ApiResponse] A target [ApiResponse].
         */
        @PublishedApi
        @Suppress("UNCHECKED_CAST")
        internal fun <T> ApiResponse<T>.operate(): ApiResponse<T> = apply {
            val globalOperators = KSandwichInitializer.sandwichOperators
            globalOperators.forEach { globalOperator ->
                if (globalOperator is ApiResponseOperator<*>) {
                    operator(globalOperator as ApiResponseOperator<T>)
                } else if (globalOperator is ApiResponseSuspendOperator<*>) {
                    val scope = KSandwichInitializer.sandwichScope
                    scope.launch {
                        suspendOperator(globalOperator as ApiResponseSuspendOperator<T>)
                    }
                }
            }
        }

        fun getStatusCodeFromResponse(response: HttpResponse): StatusCode {
            return StatusCode.values().find { it.code == response.status.value }
                ?: StatusCode.Unknown
        }
    }
}




