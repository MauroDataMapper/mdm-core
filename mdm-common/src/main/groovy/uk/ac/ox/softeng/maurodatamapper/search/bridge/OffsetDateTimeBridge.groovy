package uk.ac.ox.softeng.maurodatamapper.search.bridge

import org.hibernate.search.bridge.StringBridge
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * @since 20/07/2018
 */
class OffsetDateTimeBridge implements StringBridge {

    Logger logger = LoggerFactory.getLogger(OffsetDateTimeBridge)

    static DateTimeFormatter dtf = DateTimeFormatter.BASIC_ISO_DATE

    @Override
    String objectToString(Object object) {
        if (object instanceof OffsetDateTime) {
            return ((OffsetDateTime) object).format(dtf)
        }
        logger.error('Bridge set up to convert object of type {} but it is not an OffsetDateTime', object.getClass())
        return null
    }
}
