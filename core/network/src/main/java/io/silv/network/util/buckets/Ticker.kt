package io.silv.network.util.buckets


abstract class KTicker
/**
 * Constructor for use by subclasses.
 */
protected constructor() {
    /**
     * Returns the number of nanoseconds elapsed since this ticker's fixed
     * point of reference.
     */
    abstract fun read(): Long

    companion object {
        /**
         * A ticker that reads the current time using [System.nanoTime].
         *
         * @since 10.0
         */
        fun systemTicker(): KTicker {
            return SYSTEM_TICKER
        }

        private val SYSTEM_TICKER: KTicker = object : KTicker() {
            override fun read(): Long {
                return System.nanoTime()
            }
        }
    }
}

