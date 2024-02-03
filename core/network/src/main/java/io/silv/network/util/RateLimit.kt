package io.silv.network.util

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import kotlinx.coroutines.ensureActive
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.isomorphism.util.TokenBuckets
import java.io.IOException
import java.util.concurrent.TimeUnit

fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Int = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) = addInterceptor(RateLimitInterceptor(permits.toLong(), period.toLong(), unit))

fun HttpClient.rateLimit(
    permits: Int,
    period: Int = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): HttpClient {
    val bucket =
        TokenBuckets
            .builder()
            .withCapacity(permits.toLong())
            .withFixedIntervalRefillStrategy(permits.toLong(), period.toLong(), unit)
            .build()

    this.plugin(HttpSend).intercept {  request ->

        coroutineContext.ensureActive()

        bucket.consume()

        coroutineContext.ensureActive()

        execute(request)
    }

    return this
}

private class RateLimitInterceptor(
    permits: Long,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) : Interceptor {
    private val bucket =
        TokenBuckets
            .builder()
            .withCapacity(permits)
            .withFixedIntervalRefillStrategy(permits, period, unit)
            .build()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.call().isCanceled()) {
            throw IOException()
        }
        bucket.consume()

        if (chain.call().isCanceled()) {
            throw IOException()
        }
        return chain.proceed(chain.request())
    }
}
