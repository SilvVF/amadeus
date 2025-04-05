package io.silv.common.log

import java.io.PrintWriter
import java.io.StringWriter


/**
 * A tiny Kotlin API for cheap logging on top of Android's normal `Log` class.
 *
 * The [logcat] function has 3 parameters: an optional [priority], an optional [tag], and a required
 * string producing lambda ([message]). The lambda is only evaluated if a logger is installed and
 * the logger deems the priority loggable.
 *
 * The priority defaults to [LogPriority.DEBUG].
 *
 * The tag defaults to the class name of the log call site, without any extra runtime cost. This works
 * because [logcat] is an inlined extension function of [Any] and has access to [this] from which
 * it can extract the class name. If logging from a standalone function which has no [this], use the
 * [logcat] overload which requires a tag parameter.
 *
 * The [logcat] function does not take a [Throwable] parameter. Instead, the library provides
 * a Throwable extension function: [asLog] which returns a loggable string.
 *
 * ```
 * import logcat.LogPriority.INFO
 * import logcat.asLog
 * import logcat.logcat
 *
 * class MouseController {
 *
 *   fun play {
 *     var state = "CHEEZBURGER"
 *     logcat { "I CAN HAZ $state?" }
 *     // logcat output: D/MouseController: I CAN HAZ CHEEZBURGER?
 *
 *     logcat(INFO) { "DID U ASK 4 MOAR INFO?" }
 *     // logcat output: I/MouseController: DID U ASK 4 MOAR INFO?
 *
 *     logcat { exception.asLog() }
 *     // logcat output: D/MouseController: java.lang.RuntimeException: FYLEZ KERUPTED
 *     //                        at sample.MouseController.play(MouseController.kt:22)
 *     //                        ...
 *
 *     logcat("Lolcat") { "OH HI" }
 *     // logcat output: D/Lolcat: OH HI
 *   }
 * }
 * ```
 *
 * To install a logger, see [LogcatLogger].
 */
inline fun Any.logcat(
    priority: LogPriority = LogPriority.DEBUG,
    /**
     * If provided, the log will use this tag instead of the simple class name of `this` at the call
     * site.
     */
    tag: String? = null,
    message: () -> String
) {
    LogcatLogger.logger.let { logger ->
        if (logger.isLoggable(priority)) {
            val tagOrCaller = tag ?: outerClassSimpleNameInternalOnlyDoNotUseKThxBye()
            logger.log(priority, tagOrCaller, message())
        }
    }
}

/**
 * An overload for logging that does not capture the calling code as tag. This should only
 * be used in standalone functions where there is no `this`.
 * @see logcat above
 */
inline fun logcat(
    tag: String,
    priority: LogPriority = LogPriority.DEBUG,
    message: () -> String
) {
    with(LogcatLogger.logger) {
        if (isLoggable(priority)) {
            log(priority, tag, message())
        }
    }
}

/**
 * Logger that [logcat] delegates to. Call [install] to install a new logger, the default is a
 * no-op logger. Calling [uninstall] falls back to the default no-op logger.
 */
interface LogcatLogger {

    /**
     * Whether a log with the provided priority should be logged and the corresponding message
     * providing lambda evaluated. Called by [logcat].
     */
    fun isLoggable(priority: LogPriority) = true

    /**
     * Write a log to its destination. Called by [logcat].
     */
    fun log(
        priority: LogPriority,
        tag: String,
        message: String
    )

    companion object {
        @Volatile
        @PublishedApi
        internal var logger: LogcatLogger = NoLog
            private set

        @Volatile
        private var installedThrowable: Throwable? = null

        val isInstalled: Boolean
            get() = installedThrowable != null

        /**
         * Installs a [LogcatLogger].
         *
         * It is an error to call [install] more than once without calling [uninstall] in between,
         * however doing this won't throw, it'll log an error to the newly provided logger.
         */
        fun install(logger: LogcatLogger) {
            synchronized(this) {
                if (isInstalled) {
                    logger.log(
                        LogPriority.ERROR,
                        "LogcatLogger",
                        "Installing $logger even though a logger was previously installed here: " +
                                installedThrowable!!.asLog()
                    )
                }
                installedThrowable = RuntimeException("Previous logger installed here")
                Companion.logger = logger
            }
        }

        /**
         * Replaces the current logger (if any) with a no-op logger.
         */
        fun uninstall() {
            synchronized(this) {
                installedThrowable = null
                logger = NoLog
            }
        }
    }
    /**
     * A [LogcatLogger] that always logs and delegates to [println] concatenating
     * the tag and message, separated by a space. Alternative to [AndroidLogcatLogger]
     * when running on a JVM.
     */
    object PrintLogger : LogcatLogger {

        override fun log(priority: LogPriority, tag: String, message: String) {
            println("$tag $message")
        }
    }

    private object NoLog : LogcatLogger {
        override fun isLoggable(priority: LogPriority) = false

        override fun log(
            priority: LogPriority,
            tag: String,
            message: String
        ) = error("Should never receive any log")
    }
}

/**
 * Utility to turn a [Throwable] into a loggable string.
 *
 * The implementation is based on Timber.getStackTraceString(). It's different
 * from [android.util.Log.getStackTraceString] in the following ways:
 * - No silent swallowing of UnknownHostException.
 * - The buffer size is 256 bytes instead of the default 16 bytes.
 */
fun Throwable.asLog(): String {
    val stringWriter = StringWriter(256)
    val printWriter = PrintWriter(stringWriter, false)
    printStackTrace(printWriter)
    printWriter.flush()
    return stringWriter.toString()
}


@PublishedApi
internal fun Any.outerClassSimpleNameInternalOnlyDoNotUseKThxBye(): String {
    val javaClass = this::class.java
    val fullClassName = javaClass.name
    val outerClassName = fullClassName.substringBefore('$')
    val simplerOuterClassName = outerClassName.substringAfterLast('.')
    return if (simplerOuterClassName.isEmpty()) {
        fullClassName
    } else {
        simplerOuterClassName.removeSuffix("Kt")
    }
}

enum class LogPriority(
    val priorityInt: Int
) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6),
    ASSERT(7);
}
