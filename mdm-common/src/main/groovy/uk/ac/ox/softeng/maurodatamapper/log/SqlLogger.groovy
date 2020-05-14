package uk.ac.ox.softeng.maurodatamapper.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @since 06/02/2018
 */
class SqlLogger {

    private static final Logger logger = LoggerFactory.getLogger('org.hibernate.SQL')

    static void debug(String format, Object... arguments) {
        logger.debug(format, arguments)
    }

    static void debug(String message) {
        logger.debug(message)
    }
}
