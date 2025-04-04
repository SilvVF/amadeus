package io.silv.network.util.bucket

import kotlinx.datetime.Clock
import kotlinx.datetime.asTimeSource
import kotlin.time.ExperimentalTime


interface  Ticker {
    fun read(): Long

    companion object {
        /**
         * A ticker that reads the current time using [platformNanoTime].
         *
         * @since 10.0
         */
        fun systemTicker(): Ticker {
            return SYSTEM_TICKER
        }

        @OptIn(ExperimentalTime::class)
        private val SYSTEM_TICKER: Ticker = object : Ticker {

            val timeSource by lazy { Clock.System.asTimeSource().markNow() }

            override fun read(): Long {
                return timeSource.elapsedNow().inWholeNanoseconds
            }
        }
    }
}


