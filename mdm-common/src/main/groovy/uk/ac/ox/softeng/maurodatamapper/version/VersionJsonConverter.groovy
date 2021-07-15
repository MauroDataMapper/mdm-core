package uk.ac.ox.softeng.maurodatamapper.version


import grails.plugin.json.builder.JsonGenerator

/**
 * @since 15/07/2021
 */
class VersionJsonConverter implements JsonGenerator.Converter {
    @Override
    boolean handles(Class<?> type) {
        Version.isAssignableFrom(type)
    }

    @Override
    Object convert(Object value, String key) {
        ((Version) value).toString()
    }
}
