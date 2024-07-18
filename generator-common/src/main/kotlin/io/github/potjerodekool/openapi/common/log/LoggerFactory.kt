package io.github.potjerodekool.openapi.common.log

import java.util.function.Function

/**
 * A logger factory to create Logger instances.
 * Build systems should set the logger provider like this:
 * LoggerFactory#setLoggerProvider(mavenLoggerProvider)
 */
class LoggerFactory private constructor() {
    private var loggerProvider = Function { name: String -> devNullLoggerProvider(name) }

    companion object {
        private val FACTORY = LoggerFactory()

        fun setLoggerProvider(loggerProvider: Function<String, Logger>) {
            FACTORY.loggerProvider = loggerProvider
        }

        fun getLogger(name: String): Logger {
            return FACTORY.loggerProvider.apply(name)
        }

        private fun devNullLoggerProvider(name: String): Logger {
            return DevNullLogger()
        }
    }
}

class DevNullLogger : Logger {
    override fun log(level: LogLevel?, message: String?, exception: Throwable?) {
        //Nothing to do here
    }
}