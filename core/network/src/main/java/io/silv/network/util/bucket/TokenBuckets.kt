package io.silv.network.util.bucket

import kotlinx.coroutines.delay

/** Static utility methods pertaining to creating [TokenBucketImpl] instances.  */
public object TokenBuckets {

    /** Create a new builder for token buckets.  */
    fun builder(): Builder {
        return Builder()
    }

    private val YIELDING_SLEEP_STRATEGY: TokenBucket.SleepStrategy =
        object : TokenBucket.SleepStrategy {
            override suspend fun sleep() {
                // Sleep for the smallest unit of time possible
                // internally anything less than 1 millisecond will be rounded
                delay(1)
            }
        }

    class Builder {
        private var capacity: Long? = null
        private var initialTokens: Long = 0
        private var refillStrategy: TokenBucket.RefillStrategy? = null
        private var sleepStrategy = YIELDING_SLEEP_STRATEGY
        private val ticker = Ticker.systemTicker()

        /** Specify the overall capacity of the token bucket.  */
        fun withCapacity(numTokens: Long): Builder {
            assert(numTokens > 0)  { "Must specify a positive number of tokens" }
            capacity = numTokens
            return this
        }

        /** Initialize the token bucket with a specific number of tokens.  */
        fun withInitialTokens(numTokens: Long): Builder {
            assert(numTokens > 0) { "Must specify a positive number of tokens" }
            initialTokens = numTokens
            return this
        }

        /** Refill tokens at a fixed interval.  */
        fun withFixedIntervalRefillStrategy(
            refillTokens: Long,
            periodNanos: Long,
        ): Builder {
            return withRefillStrategy(
                FixedIntervalRefillStrategy(
                    ticker,
                    refillTokens,
                    periodNanos,
                )
            )
        }

        /** Use a user defined refill strategy.  */
        fun withRefillStrategy(refillStrategy: TokenBucket.RefillStrategy): Builder {
            this.refillStrategy = refillStrategy
            return this
        }

        /** Build the token bucket.  */
        fun build(): TokenBucket {

            checkNotNull(capacity) { "Must specify a capacity" }
            checkNotNull(refillStrategy) { "Must specify a refill strategy" }

            return TokenBucketImpl(
                capacity!!,
                initialTokens,
                refillStrategy!!,
                sleepStrategy)
        }
    }
}