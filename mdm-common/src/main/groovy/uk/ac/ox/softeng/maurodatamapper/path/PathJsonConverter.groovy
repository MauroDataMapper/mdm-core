package uk.ac.ox.softeng.maurodatamapper.path

import grails.plugin.json.builder.JsonGenerator

/**
 * @since 15/07/2021
 */
class PathJsonConverter implements JsonGenerator.Converter {
    @Override
    boolean handles(Class<?> type) {
        Path.isAssignableFrom(type)
    }

    @Override
    Object convert(Object value, String key) {
        ((Path) value).toString()
    }
}
