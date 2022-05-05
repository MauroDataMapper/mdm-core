package uk.ac.ox.softeng.maurodatamapper.federation.converter

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.maurodatamapper.federation.web.FederationClient
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.rest.Link
import groovy.xml.slurpersupport.GPathResult

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class AtomSubscribedCatalogueConverter implements SubscribedCatalogueConverter {
    @Override
    boolean handles(SubscribedCatalogueType type) {
        type == SubscribedCatalogueType.ATOM
    }

    @Override
    Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        GPathResult subscribedCatalogueModelsFeed = federationClient.withCloseable {client ->
            client.getSubscribedCatalogueModelsFromAtomFeed(subscribedCatalogue.apiKey)
        }

        Authority subscribedAuthority = new Authority(label: subscribedCatalogueModelsFeed.author.name, url: subscribedCatalogueModelsFeed.author.uri)

        List<PublishedModel> publishedModels = subscribedCatalogueModelsFeed.entry.collect {entry ->
            new PublishedModel().tap {
                modelId = Utils.toUuid(extractUuidFromUrn(entry.id.text()))
                title = entry.title
                // modelType = entry.category.@term
                lastUpdated = OffsetDateTime.parse(entry.updated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                datePublished = OffsetDateTime.parse(entry.published.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                author = entry.author.name
                description = entry.summary
                links = entry.link.collect {link ->
                    new Link(LINK_RELATIONSHIP_ALTERNATE, link.@href.text()).tap {contentType = link.@type}
                }
            }
        }

        return new Tuple2(subscribedAuthority, publishedModels)
    }

    private static String extractUuidFromUrn(String url) {
        final String separator = ':'
        int lastPos = url.lastIndexOf(separator)

        return url.substring(lastPos + 1)
    }
}