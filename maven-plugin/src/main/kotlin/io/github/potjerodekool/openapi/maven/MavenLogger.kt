package io.github.potjerodekool.openapi.maven

import io.github.potjerodekool.openapi.common.log.LogLevel
import io.github.potjerodekool.openapi.common.log.Logger
import org.apache.maven.plugin.Mojo

class MavenLogger(
    private val mojo: Mojo,
    private val name: String
) : Logger {
    override fun log(level: LogLevel?, message: String?, exception: Throwable?) {
        val logMessage = String.format("%s: %s", name, message)

        when (level) {
            LogLevel.SEVERE -> mojo.log.error(logMessage, exception)
            LogLevel.WARNING -> mojo.log.warn(logMessage, exception)
            else -> mojo.log.info(logMessage, exception)
        }
    }
}
