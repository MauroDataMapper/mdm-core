package uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json

import grails.plugin.json.builder.JsonGenerator
import org.springframework.core.Ordered

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * @since 02/10/2017
 */
class OffsetDateTimeConverter implements JsonGenerator.Converter, Ordered {

    @Override
    boolean handles(Class<?> type) {
        OffsetDateTime.isAssignableFrom(type)
    }

    @Override
    Object convert(Object value, String key) {
        toString((OffsetDateTime) value)
    }

    static String toString(OffsetDateTime offsetDateTime) {
        offsetDateTime?.withOffsetSameInstant(ZoneOffset.UTC)?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE
    }
}
