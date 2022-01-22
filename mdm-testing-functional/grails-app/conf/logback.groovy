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
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.Charset

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

// Message
String ansiPattern = '%clr(%d{ISO8601}){faint} ' + // Date
                     '%clr([%10.10thread]){faint} ' + // Thread
                     '%clr(%-5level) ' + // Log level
                     '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                     '%m%n%wex'
// Message

String nonAnsiPattern = '%d{ISO8601} [%10.10thread] %-5level %-40.40logger{39} : %msg%n'

String cIProp = System.getProperty('mdm.ciMode')
boolean ciEnv = cIProp ? cIProp.toBoolean() : false

def baseDir = Environment.current == Environment.PRODUCTION ? BuildSettings.BASE_DIR.canonicalFile : BuildSettings.TARGET_DIR.canonicalFile
def clazz = Environment.current == Environment.PRODUCTION ? RollingFileAppender : FileAppender

File logDir = new File(baseDir, 'logs').canonicalFile
String logFilename = System.getProperty('mdm.logFileName') ?: Environment.current == Environment.PRODUCTION ? baseDir.name : baseDir.parentFile.name


// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')
        pattern = ciEnv || Environment.current == Environment.TEST ? nonAnsiPattern : ansiPattern
    }

    if (Environment.current == Environment.TEST) {
        filter(ThresholdFilter) {
            level = INFO
        }
    } else {
        filter(ThresholdFilter) {
            level = WARN
        }
    }
    filter(HibernateMappingFilter)
    filter HibernateDeprecationFilter
    filter HibernateNarrowingFilter
}

appender("FILE", clazz) {
    file = "${logDir}/${logFilename}.log"
    append = false

    encoder(PatternLayoutEncoder) {
        pattern = nonAnsiPattern
    }
    filter(HibernateMappingFilter)
    filter HibernateDeprecationFilter
    filter HibernateNarrowingFilter

    if (clazz == RollingFileAppender) {
        rollingPolicy(TimeBasedRollingPolicy) {
            maxHistory = 90
            fileNamePattern = "${logDir}/${logFilename}.%d{yyyy-MM-dd}.log"
        }
    }
    filter(ThresholdFilter) {
        level = TRACE
    }
}
root(INFO, ['STDOUT', 'FILE'])


logger('uk.ac.ox.softeng', DEBUG)
logger('db.migration', DEBUG)

logger('org.springframework.jdbc.core.JdbcTemplate', DEBUG)

logger('org.apache.lucene', DEBUG)
logger('org.hibernate.search.fulltext_query', DEBUG)
logger('org.hibernate.search.batchindexing.impl', WARN)
//    logger('org.hibernate.SQL', DEBUG)
// logger 'org.hibernate.type', TRACE
logger('org.flyway', DEBUG)
// Track interceptor order
logger 'grails.artefact.Interceptor', DEBUG
logger 'org.simplejavamail.mailer.internal.mailsender.MailSender', OFF

logger('org.grails.spring.beans.factory.OptimizedAutowireCapableBeanFactory', ERROR)
logger('org.springframework.context.support.PostProcessorRegistrationDelegate', WARN)
logger('org.hibernate.cache.ehcache.AbstractEhcacheRegionFactory', ERROR)
logger 'org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl', ERROR
logger 'org.hibernate.engine.jdbc.spi.SqlExceptionHelper', ERROR

logger 'org.springframework.mock.web.MockServletContext', ERROR
logger 'StackTrace', OFF

class HibernateMappingFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /.*Specified config option \[importFrom\].*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}

class HibernateDeprecationFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /HHH90000022.*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}

class HibernateNarrowingFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /HHH000179.*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}