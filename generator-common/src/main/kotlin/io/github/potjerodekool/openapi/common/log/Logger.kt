package io.github.potjerodekool.openapi.common.log

/**
 * A logger interface to allow logging to the log of the buildsystems.
 */
interface Logger {
    fun info(message: String?) {
        log(LogLevel.INFO, message)
    }

    fun log(level: LogLevel?, message: String?) {
        this.log(level, message, null)
    }

    fun log(level: LogLevel?, message: String?, exception: Throwable?)

    companion object {
        fun getLogger(name: String): Logger {
            return LoggerFactory.getLogger(name)
        }
    }
}
