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
        GPathResult subscribedCatalogueModelsFeed = federationClient.getSubscribedCatalogueModelsFromAtomFeed(subscribedCatalogue.apiKey)

        Authority subscribedAuthority = new Authority(label: subscribedCatalogueModelsFeed.author.name.text(), url: subscribedCatalogueModelsFeed.author.uri.text())

        List<PublishedModel> publishedModels = subscribedCatalogueModelsFeed.entry.collect {entry ->
            new PublishedModel().tap {
                modelId = entry.id
                title = entry.title
                if (entry.updated.text()) lastUpdated = OffsetDateTime.parse(entry.updated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                if (entry.published.text()) datePublished = OffsetDateTime.parse(entry.published.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                author = entry.author.name ?: subscribedCatalogueModelsFeed.author.name
                description = entry.summary
                links = entry.link.collect {link ->
                    new Link(LINK_RELATIONSHIP_ALTERNATE, link.@href.text()).tap {contentType = link.@type}
                }
            }
        }

        return new Tuple2(subscribedAuthority, publishedModels)
    }
}