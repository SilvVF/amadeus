package io.silv.network.util.buckets

import kotlinx.coroutines.delay

/** Static utility methods pertaining to creating [TokenBucketImpl] instances.  */
object TokenBuckets {

    /** Create a new builder for token buckets.  */
    fun builder(): Builder {
        return Builder()
    }

    private val YIELDING_SLEEP_STRATEGY: TokenBucket.SleepStrategy =
        object : TokenBucket.SleepStrategy {
            override suspend fun sleep() {
                // Sleep for the smallest unit of time possible just to relinquish control
                // and to allow other threads to run.
                delay(1)
            }
        }
    private val BUSY_WAIT_SLEEP_STRATEGY: TokenBucket.SleepStrategy =
        object : TokenBucket.SleepStrategy {
            override suspend fun sleep() {
                // Do nothing, don't sleep.
            }
        }

    class Builder {
        private var capacity: Long? = null
        private var initialTokens: Long = 0
        private var refillStrategy: TokenBucket.RefillStrategy? = null
        private var sleepStrategy = YIELDING_SLEEP_STRATEGY
        private val ticker = KTicker.systemTicker()

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

        /** Use a sleep strategy that will always attempt to yield the CPU to other processes.  */
        fun withYieldingSleepStrategy(): Builder {
            return withSleepStrategy(YIELDING_SLEEP_STRATEGY)
        }

        /**
         * Use a sleep strategy that will not yield the CPU to other processes.  It will busy wait until more tokens become
         * available.
         */
        fun withBusyWaitSleepStrategy(): Builder {
            return withSleepStrategy(BUSY_WAIT_SLEEP_STRATEGY)
        }

        /** Use a user defined sleep strategy.  */
        fun withSleepStrategy(sleepStrategy: TokenBucket.SleepStrategy): Builder {
            this.sleepStrategy = sleepStrategy
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