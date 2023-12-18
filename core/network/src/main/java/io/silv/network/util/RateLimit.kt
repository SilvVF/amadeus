package io.silv.network.util

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.isomorphism.util.TokenBuckets

fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Int = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) = addInterceptor(RateLimitInterceptor(permits.toLong(), period.toLong(), unit))

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
