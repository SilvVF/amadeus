package io.silv.network.util

import android.util.Log
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.ktor.headers
import com.skydoves.sandwich.ktor.statusCode
import com.skydoves.sandwich.operators.ApiResponseSuspendOperator
import io.ktor.util.toMap

class MangaDexApiLogger<T> : ApiResponseSuspendOperator<T>() {
    private val tag = "MangaDexApiLogger"

    override suspend fun onSuccess(apiResponse: ApiResponse.Success<T>) {
        Log.d(
            tag,
            """
                [onSuccess]
                (apiResponse)
                headers:${apiResponse.headers.toMap()}, 
                statusCode:${apiResponse.statusCode}
               
               ${apiResponse.data.toString()}
                """,
        )
    }

    override suspend fun onError(apiResponse: ApiResponse.Failure.Error) {
        Log.d(tag, "[onError](apiResponse=$apiResponse)")
    }

    override suspend fun onException(apiResponse: ApiResponse.Failure.Exception) {
        Log.d(tag, "[onException](apiResponse=$apiResponse)")
    }
}
