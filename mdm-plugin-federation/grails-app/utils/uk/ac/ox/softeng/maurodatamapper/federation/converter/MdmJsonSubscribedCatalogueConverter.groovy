package uk.ac.ox.softeng.maurodatamapper.federation.converter

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.maurodatamapper.federation.web.FederationClient
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.rest.Link

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class MdmJsonSubscribedCatalogueConverter implements SubscribedCatalogueConverter {
    @Override
    boolean handles(SubscribedCatalogueType type) {
        type == SubscribedCatalogueType.MDM_JSON
    }

    @Override
    Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        Map<String, Object> subscribedCatalogueModels = federationClient.withCloseable {client ->
            client.getSubscribedCatalogueModels(subscribedCatalogue.apiKey)
        }

        Authority subscribedAuthority = new Authority(label: subscribedCatalogueModels.authority.label, url: subscribedCatalogueModels.authority.url)

        List<PublishedModel> publishedModels = (subscribedCatalogueModels.publishedModels as List<Map<String, Object>>).collect {pm ->
            new PublishedModel().tap {
                modelId = Utils.toUuid(pm.modelId)
                title = pm.title // for compatibility with remote catalogue versions prior to 4.12
                if (pm.label) modelLabel = pm.label
                if (pm.version) modelVersion = Version.from(pm.version)
                modelType = pm.modelType
                lastUpdated = OffsetDateTime.parse(pm.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                dateCreated = OffsetDateTime.parse(pm.dateCreated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                datePublished = OffsetDateTime.parse(pm.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                author = pm.author
                description = pm.description
                if (pm.links) links = pm.links.collect {link -> new Link(LINK_RELATIONSHIP_ALTERNATE, link.url).tap {contentType = link.contentType}}
            }
        }

        return new Tuple2(subscribedAuthority, publishedModels)
    }
}
