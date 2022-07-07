/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.core.logback

import uk.ac.ox.softeng.maurodatamapper.core.logback.filter.HibernateLogFilter

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.spi.ContextAwareBase
import ch.qos.logback.core.util.StatusPrinter
import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.LoggingSystem
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.Charset

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.OFF
import static ch.qos.logback.classic.Level.TRACE
import static ch.qos.logback.classic.Level.WARN

/**
 *  https://logback.qos.ch/manual/configuration.html
 *  It takes about 100 miliseconds for Joran to parse a given logback configuration file.
 *  To shave off those miliseconds at aplication start up, you can use the service-provider loading facility (item 4 above)
 *  to load your own custom Configurator class with BasicConfigurator serving as a good starting point.
 *
 *  Given that Groovy is a full-fledged language, we have dropped support for logback.groovy in order to protect the innocent.
 * @since 01/02/2022
 */
class MdmConfigurator extends ContextAwareBase implements Configurator {

    // Message
    static final String ansiPattern = '%clr(%d{ISO8601}){faint} ' + // Date
                                      '%clr([%10.10thread]){faint} ' + // Thread
                                      '%clr(%-5level) ' + // Log level
                                      '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                                      '%m%n%wex'
    // Message

    static final String nonAnsiPattern = '%d{ISO8601} [%10.10thread] %-5level %-40.40logger{39} : %msg%n'
    static final String DEFAULT_LOG_FILENAME = 'mauro-data-mapper'

    boolean ciEnv
    boolean testEnv
    boolean prodEnv
    boolean runningInTomcat
    boolean enableSqlLogging
    boolean enableSqlParameterLogging
    boolean compiling = false
    File baseDir

    MdmConfigurator() {
        ciEnv = getBooleanSystemProperty('mdm.ciMode') ?: System.hasProperty('JENKINS') ?: false
        enableSqlLogging = getBooleanSystemProperty('logging.sql')
        enableSqlParameterLogging = getBooleanSystemProperty('logging.sql.parameters')

        String tomcatHome = System.getProperty('catalina.home') ?: System.getenv('CATALINA_HOME')
        runningInTomcat = tomcatHome
        baseDir = runningInTomcat ? new File(tomcatHome) : prodEnv ? BuildSettings.BASE_DIR.canonicalFile : BuildSettings.TARGET_DIR.canonicalFile

        // Custom handling for the times micronaut instantiates logging whilst compiling
        if (BuildSettings.TARGET_DIR.canonicalPath.contains('gradle/workers')) {
            compiling = true
        } else {
            try {
                testEnv = Environment.current == Environment.TEST
                prodEnv = Environment.current == Environment.PRODUCTION
            } catch (Exception ignored) {
                // Custom handling for the times micronaut instantiates logging whilst compiling
                compiling = true
            }
        }
    }

    void configure(LoggerContext lc) {
        if (compiling) return
        String mode = ciEnv ? 'Jenkins' : testEnv ? 'Test' : prodEnv ? 'Production' : 'Development'
        addInfo("Setting up default MDM configuration in [${mode}] mode")

        addConversionRule 'clr', ColorConverter
        addConversionRule 'wex', WhitespaceThrowableProxyConverter

        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.addAppender(createConsoleAppender(lc))
        rootLogger.addAppender(createFileAppender(lc))
        rootLogger.setLevel(INFO)
        configureLogLevels()

        // This stops spring boot from initialising its own logging
        // Otherwise spring just overrides anything we do here as its coded to only not do this if a xml or groovy file exist
        lc.putObject(LoggingSystem.getName(), new Object())
        addInfo('Disabled Spring Boot Logging Control')

        StatusPrinter.print(lc)
    }

    void configureLogLevels() {

        if (!prodEnv) {

            setLoggerLevel('uk.ac.ox.softeng', TRACE)
            setLoggerLevel('db.migration', DEBUG)
            setLoggerLevel('org.flyway', DEBUG)

            setLoggerLevel('org.springframework.jdbc.core.JdbcTemplate', DEBUG)
            setLoggerLevel('org.apache.lucene', DEBUG)
            setLoggerLevel('org.hibernate.search.fulltext_query', DEBUG)
            setLoggerLevel('org.hibernate.search.batchindexing.impl', WARN)

            if (enableSqlLogging) {
                setLoggerLevel('org.hibernate.SQL', DEBUG)
            }
            if (enableSqlParameterLogging) {
                setLoggerLevel 'org.hibernate.type', TRACE
            }

            // Track interceptor order
            // setLoggerLevel 'grails.artefact.Interceptor', DEBUG
        }

        setLoggerLevel('org.grails.spring.beans.factory.OptimizedAutowireCapableBeanFactory', ERROR)
        setLoggerLevel('org.springframework.context.support.PostProcessorRegistrationDelegate', WARN)
        setLoggerLevel('org.hibernate.cache.ehcache.AbstractEhcacheRegionFactory', ERROR)
        setLoggerLevel 'org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl', ERROR
        setLoggerLevel 'org.hibernate.engine.jdbc.spi.SqlExceptionHelper', ERROR

        setLoggerLevel 'org.springframework.mock.web.MockServletContext', ERROR
        setLoggerLevel 'StackTrace', OFF
    }

    ConsoleAppender createConsoleAppender(LoggerContext lc) {
        addInfo('Creating ConsoleAppender with name [STDOUT]')
        new ConsoleAppender<ILoggingEvent>().tap {
            setContext(lc)
            setName('STDOUT')
            setEncoder(createPatternEncoder(lc, ciEnv || testEnv ? nonAnsiPattern : ansiPattern))
            addFilter(new ThresholdFilter().tap {
                setLevel(testEnv ? 'INFO' : 'WARN')
            })
            addFilter(new HibernateLogFilter())
            copyOfAttachedFiltersList.each {it.start()}
            start()
        }
    }

    FileAppender createFileAppender(LoggerContext lc) {
        addInfo('Creating FileAppender with name [FILE]')

        File logDir = runningInTomcat ? new File(baseDir, '/logs/mauro-data-mapper') : new File(baseDir, 'logs')
        if (!logDir) logDir.mkdirs()
        addInfo("Using log directory for logs ${logDir}")

        String logFileName = System.getProperty('mdm.logFileName') ?: prodEnv ? DEFAULT_LOG_FILENAME : baseDir.parentFile.name
        FileAppender<ILoggingEvent> appender
        TimeBasedRollingPolicy timeBasedRollingPolicy

        if (prodEnv) {
            addInfo('Using RollingFileAppender rather than FileAppender')
            appender = new RollingFileAppender<ILoggingEvent>()
            timeBasedRollingPolicy = new TimeBasedRollingPolicy().tap {
                setContext(lc)
                setMaxHistory(90)
                setFileNamePattern("${logDir}/${logFileName}.%d{yyyy-MM-dd}.log.gz")
                setParent(appender)
            }
        } else {
            appender = new FileAppender<ILoggingEvent>()
        }

        appender.tap {
            setContext(lc)
            setName('FILE')
            setEncoder(createPatternEncoder(lc, nonAnsiPattern))
            setAppend(false)
            setFile("${logDir}/${logFileName}.log")
            addFilter(new ThresholdFilter().tap {
                setLevel('TRACE')
            })
            addFilter(new HibernateLogFilter())
            copyOfAttachedFiltersList.each {it.start()}
        }

        if (appender instanceof RollingFileAppender) {
            appender.tap {
                setRollingPolicy(timeBasedRollingPolicy)
                setTriggeringPolicy(timeBasedRollingPolicy)
                setAppend(true)
            }
            timeBasedRollingPolicy.start()
        }
        appender.tap {start()}
    }

    void addConversionRule(String conversionWord, Class converterClass) {
        String converterClassName = converterClass.getName()

        Map<String, String> ruleRegistry = (Map) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY)
        if (ruleRegistry == null) {
            ruleRegistry = [:]
            context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, ruleRegistry)
        }
        // put the new rule into the rule registry
        addInfo('Registering conversion word ' + conversionWord + ' with class [' + converterClassName + ']')
        ruleRegistry[conversionWord] = converterClassName
    }

    PatternLayoutEncoder createPatternEncoder(LoggerContext lc, String pattern) {
        new PatternLayoutEncoder().tap {
            setCharset(Charset.forName('UTF-8'))
            setContext(lc)
            setPattern(pattern)
            start()
        }
    }

    void setLoggerLevel(String name, Level level) {
        Logger logger = ((LoggerContext) context).getLogger(name)
        addInfo("Setting level of logger [${name}] to ${level}")
        logger.level = level
    }

    boolean getBooleanSystemProperty(String propertyName) {
        String prop = System.getProperty(propertyName)
        prop ? prop.toBoolean() : false
    }
}
