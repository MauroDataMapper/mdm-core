package uk.ac.ox.softeng.maurodatamapper.util


import grails.plugin.json.builder.JsonGenerator

/**
 * @since 15/07/2021
 */
class UuidJsonConverter implements JsonGenerator.Converter {
    @Override
    boolean handles(Class<?> type) {
        UUID.isAssignableFrom(type)
    }

    @Override
    Object convert(Object value, String key) {
        ((UUID) value).toString()
    }
}
