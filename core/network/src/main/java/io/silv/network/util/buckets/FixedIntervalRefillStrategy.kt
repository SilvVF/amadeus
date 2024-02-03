package io.silv.network.util.buckets


/**
 * A token bucket refill strategy that will provide N tokens for a token bucket to consume every T units of time.
 * The tokens are refilled in bursts rather than at a fixed rate.  This refill strategy will never allow more than
 * N tokens to be consumed during a window of time T.
 */
class FixedIntervalRefillStrategy(
    private val ticker: KTicker,
    private val numTokensPerPeriod: Long,
    private val periodNanos: Long,
) : TokenBucket.RefillStrategy {


    private var lastRefillTime: Long = -periodNanos
    private var nextRefillTime: Long = -periodNanos


    @Synchronized
    override fun refill(): Long {
        val now =  ticker.read()
        if (now < nextRefillTime) {
            return 0
        }
        // We now know that we need to refill the bucket with some tokens, the question is how many.  We need to count how
        // many periods worth of tokens we've missed.
        val numPeriods = Math.max(0, (now - lastRefillTime) / periodNanos);

        // Move the last refill time forward by this many periods.
        lastRefillTime += numPeriods * periodNanos

        // ...and we'll refill again one period after the last time we refilled.
        nextRefillTime = lastRefillTime + periodNanos
        return numPeriods * numTokensPerPeriod
    }

    override fun getNanosUntilNextRefill(): Long {
        val now = ticker.read()
        return Math.max(0, nextRefillTime - now)
    }
}