package com.avioconsulting.util

import org.apache.maven.plugin.logging.Log

class MavenLogger implements Logger {
    private final Log mavenLogger

    MavenLogger(Log mavenLogger) {
        this.mavenLogger = mavenLogger
    }

    def info(String message) {
        this.mavenLogger.info message
    }
}
