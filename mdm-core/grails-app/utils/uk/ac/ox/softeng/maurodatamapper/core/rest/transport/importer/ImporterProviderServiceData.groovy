package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer

import grails.validation.Validateable

class ImporterProviderServiceData implements Validateable {
    String name
    String namespace
    String version

    static constraints = {
        version nullable: true
    }
}
