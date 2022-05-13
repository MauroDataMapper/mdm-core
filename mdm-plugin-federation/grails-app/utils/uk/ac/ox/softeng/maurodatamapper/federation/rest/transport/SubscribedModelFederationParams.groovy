package uk.ac.ox.softeng.maurodatamapper.federation.rest.transport

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer.ImporterProviderServiceData
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedModel

class SubscribedModelFederationParams {
    SubscribedModel subscribedModel
    String url
    String contentType
    ImporterProviderServiceData importerProviderService
}
