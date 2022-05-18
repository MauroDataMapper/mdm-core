package uk.ac.ox.softeng.maurodatamapper.federation.rest.transport

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer.ImporterProviderServiceData
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedModel

import grails.validation.Validateable

class SubscribedModelFederationParams implements Validateable {
    SubscribedModel subscribedModel
    String url
    String contentType
    ImporterProviderServiceData importerProviderService

    static constraints = {
        url nullable: true
        contentType nullable: true
        importerProviderService: true
    }
}
