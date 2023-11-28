package io.silv.network.util

import android.util.Log
import io.silv.ktor_response_mapper.ApiResponse
import io.silv.ktor_response_mapper.operators.ApiResponseSuspendOperator

class MangaDexApiLogger<T>(): ApiResponseSuspendOperator<T>() {

    private val tag = "MangaDexApi"

    override suspend fun onSuccess(apiResponse: ApiResponse.Success<T>) {
        Log.d(
            tag,
            "[onSuccess]" +
                  "(apiResponse= " +
                    "headers:${apiResponse.headers}," +
                    " statusCode:${apiResponse.statusCode}," +
                    " data:${
                        if (apiResponse.data.toString().length > 50) apiResponse.data.toString().take(50)
                        else apiResponse.data.toString()
                    })"

        )
    }

    override suspend fun onError(apiResponse: ApiResponse.Failure.Error<T>) {
        Log.d(tag, "[onError](apiResponse=$apiResponse)")
    }

    override suspend fun onException(apiResponse: ApiResponse.Failure.Exception<T>) {
        Log.d(tag, "[onException](apiResponse=${apiResponse})")
    }
}