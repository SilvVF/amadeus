package io.silv.network.util.bucket

import android.util.Log
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

/**
 * A token bucket implementation that is of a leaky bucket in the sense that it has a finite capacity and any added
 * tokens that would exceed this capacity will "overflow" out of the bucket and are lost forever.
 *
 *
 * In this implementation the rules for refilling the bucket are encapsulated in a provided `RefillStrategy`
 * instance.  Prior to attempting to consume any tokens the refill strategy will be consulted to see how many tokens
 * should be added to the bucket.
 *
 *
 * In addition in this implementation the method of yielding CPU control is encapsulated in the provided
 * `SleepStrategy` instance.  For high performance applications where tokens are being refilled incredibly quickly
 * and an accurate bucket implementation is required, it may be useful to never yield control of the CPU and to instead
 * busy wait.  This strategy allows the caller to make this decision for themselves instead of the library forcing a
 * decision.
 */
internal class TokenBucketImpl(
    /**
     * Returns the capacity of this token bucket.  This is the maximum number of tokens that the bucket can hold at
     * any one time.
     *
     * @return The capacity of the bucket.
     */
    override val capacity: Long,
    initialTokens: Long,
    private val refillStrategy: TokenBucket.RefillStrategy,
    private val sleepStrategy: TokenBucket.SleepStrategy
) :
    TokenBucket {

    private val mutex = Mutex()

    private var size: Long  = initialTokens

    init {
        assert(capacity > 0) { "capacity was less than 0" }
        assert(initialTokens <= capacity) { "initial tokens exceed capacity" }
    }

    /**
     * Returns the current number of tokens in the bucket.  If the bucket is empty then this method will return 0.
     *
     * @return The current number of tokens in the bucket.
     */
    override suspend fun numTokens(): Long {
            // Give the refill strategy a chance to add tokens if it needs to so that we have an accurate
            // count.
        return mutex.withReentrantLock {
            refill(refillStrategy.refill())
            size
        }
    }

    /**
     * Returns the amount of time in the specified time unit until the next group of tokens can be added to the token
     * bucket.
     *
     * @see TokenBucket.RefillStrategy.getNanosUntilNextRefill
     * @return The amount of time until the next group of tokens can be added to the token bucket.
     */
    @Throws(UnsupportedOperationException::class)
    override fun getNanosUntilNextRefill(): Long {
        return refillStrategy.getNanosUntilNextRefill()
    }

    /**
     * Attempt to consume a single token from the bucket.  If it was consumed then `true` is returned, otherwise
     * `false` is returned.
     *
     * @return `true` if a token was consumed, `false` otherwise.
     */
    override suspend fun tryConsume(): Boolean {
        return tryConsume(1)
    }

    /**
     * Attempt to consume a specified number of tokens from the bucket.  If the tokens were consumed then `true`
     * is returned, otherwise `false` is returned.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     * @return `true` if the tokens were consumed, `false` otherwise.
     */
    override suspend fun tryConsume(numTokens: Long): Boolean {
        return mutex.withReentrantLock {

            assert(numTokens > 0) { "Number of tokens to consume must be positive" }
            assert(numTokens <= capacity) { "Number of tokens to consume must be less than the capacity of the bucket." }

            refill(refillStrategy.refill())

            // Now try to consume some tokens
            if (numTokens <= size) {
                size -= numTokens
                true
            } else {
                false
            }
        }
    }

    /**
     * Consume a single token from the bucket.  If no token is currently available then this method will block until a
     * token becomes available.
     */
    override suspend fun consume() {
        consume(1)
    }

    /**
     * Consumes multiple tokens from the bucket.  If enough tokens are not currently available then this method will block
     * until
     *
     * @param numTokens The number of tokens to consume from teh bucket, must be a positive number.
     */
    override suspend fun consume(numTokens: Long) {
        while (coroutineContext.isActive) {
            if (tryConsume(numTokens)) {
                break
            }
            sleepStrategy.sleep()
        }
    }

    /**
     * Refills the bucket with the specified number of tokens.  If the bucket is currently full or near capacity then
     * fewer than `numTokens` may be added.
     *
     * @param numTokens The number of tokens to add to the bucket.
     */
    override suspend fun refill(numTokens: Long) {
        mutex.withReentrantLock {
            size = (size + numTokens).coerceIn(numTokens.coerceAtMost(capacity)..capacity)
        }
    }
}