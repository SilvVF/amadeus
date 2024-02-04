package io.silv.network.util

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.silv.network.util.bucket.TokenBuckets
import kotlinx.coroutines.ensureActive

fun HttpClient.rateLimit(
    permits: Int,
    seconds: Int = 1,
): HttpClient {
    val bucket =
        TokenBuckets
            .builder()
            .withCapacity(permits.toLong())
            .withFixedIntervalRefillStrategy(
                refillTokens = permits.toLong(),
                periodNanos = seconds.toLong() * 1_000_000_000L
            )
            .build()

    this.plugin(HttpSend).intercept {  request ->

        coroutineContext.ensureActive()

        bucket.consume()

        coroutineContext.ensureActive()

        execute(request)
    }

    return this
}
