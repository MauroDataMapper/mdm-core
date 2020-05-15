package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * @since 30/03/2020
 */
abstract class DateTimeSearchParamFilter implements SearchParamFilter {

    OffsetDateTime getOffsetDateTimeFromDate(Date date) {
        OffsetDateTime.ofInstant(date.toInstant(), ZoneId.from(OffsetDateTime.now()))
    }
}
