package uk.ac.ox.softeng.maurodatamapper.federation.converter

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.maurodatamapper.federation.web.FederationClient

interface SubscribedCatalogueConverter {
    String LINK_RELATIONSHIP_ALTERNATE = 'alternate'

    boolean handles(SubscribedCatalogueType type)

    Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue)

    default Authority getAuthority(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        getAuthorityAndPublishedModels(federationClient, subscribedCatalogue).v1
    }

    default List<PublishedModel> getPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        getAuthorityAndPublishedModels(federationClient, subscribedCatalogue).v2
    }
}