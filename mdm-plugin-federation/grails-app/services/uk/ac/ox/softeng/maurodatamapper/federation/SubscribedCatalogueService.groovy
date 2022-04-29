/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.AnonymisableService
import uk.ac.ox.softeng.maurodatamapper.federation.web.FederationClient
import uk.ac.ox.softeng.maurodatamapper.security.basic.AnonymousUser
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.rest.Link
import grails.util.Environment
import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.netty.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import org.springframework.beans.factory.annotation.Autowired

import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Transactional
@Slf4j
class SubscribedCatalogueService implements XmlImportMapping, AnonymisableService {

    public static final String LINK_RELATIONSHIP_ALTERNATE = 'alternate'

    @Autowired
    HttpClientConfiguration httpClientConfiguration
    @Autowired
    NettyClientSslBuilder nettyClientSslBuilder
    @Autowired
    MediaTypeCodecRegistry mediaTypeCodecRegistry

    GrailsApplication grailsApplication
    AuthorityService authorityService

    SubscribedCatalogue get(Serializable id) {
        SubscribedCatalogue.get(id)
    }

    List<SubscribedCatalogue> list(Map pagination = [:]) {
        SubscribedCatalogue.by().list(pagination)
    }

    Long count() {
        SubscribedCatalogue.count()
    }

    void delete(SubscribedCatalogue subscribedCatalogue) {
        subscribedCatalogue.delete(flush: true)
    }

    SubscribedCatalogue save(SubscribedCatalogue subscribedCatalogue) {
        subscribedCatalogue.save(failOnError: true, validate: false)
    }

    void verifyConnectionToSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        try {
            /*Map<String, Object> catalogueModels = getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
                client.getSubscribedCatalogueModels(subscribedCatalogue.apiKey)
            }
            if (!catalogueModels.containsKey('publishedModels') || !catalogueModels.authority) {
                subscribedCatalogue.errors.reject('invalid.subscription.url.response',
                                                  [subscribedCatalogue.url].toArray(),
                                                  'Invalid subscription to catalogue at [{0}], response from catalogue is invalid')
            }
            Authority thisAuthority = authorityService.defaultAuthority

            // Under prod mode dont let a connection to ourselves exist
            if (Environment.current == Environment.PRODUCTION) {
                if (catalogueModels.authority.label == thisAuthority.label && catalogueModels.authority.url == thisAuthority.url) {
                    subscribedCatalogue.errors.reject(
                        'invalid.subscription.url.authority',
                        [subscribedCatalogue.url,
                         catalogueModels.authority.label,
                         catalogueModels.authority.url].toArray(),
                        'Invalid subscription to catalogue at [{0}] as it has the same Authority as this instance [{1}:{2}]')
                }
            }*/

            def (Authority subscribedAuthority, List<PublishedModel> publishedModels) = listPublishedModels(subscribedCatalogue, true)

            Authority thisAuthority = authorityService.defaultAuthority

            // Under prod mode dont let a connection to ourselves exist
            if (Environment.current == Environment.PRODUCTION) {
                if (subscribedAuthority.label == thisAuthority.label && subscribedAuthority.url == thisAuthority.url) {
                    subscribedCatalogue.errors.reject(
                        'invalid.subscription.url.authority',
                        [subscribedCatalogue.url,
                         subscribedAuthority.label,
                         subscribedAuthority.url].toArray(),
                        'Invalid subscription to catalogue at [{0}] as it has the same Authority as this instance [{1}:{2}]')
                }
            }

        } catch (ApiException exception) {
            subscribedCatalogue.errors.reject('invalid.subscription.url.connection',
                                              [subscribedCatalogue.url, exception.message].toArray(),
                                              'Invalid subscription to catalogue at [{0}], cannot connect to catalogue due to {1}')
            log.warn('Unable to confirm catalogue subscription due to exception', exception)
        } catch (Exception exception) {
            subscribedCatalogue.errors.reject('invalid.subscription.url.exception',
                                              [subscribedCatalogue.url, exception.message].toArray(),
                                              'Invalid subscription to catalogue at [{0}] due to {1}')
            log.warn('Unable to confirm catalogue subscription due to exception', exception)
        }
        subscribedCatalogue
    }

    /**
     * Return a list of models available on the subscribed catalogue.
     * 1. Connect to the endpoint /api/published/models on the remote, authenticating by setting an api key in the header
     * 2. Return list of PublishedModels received in JSON from the remote catalogue
     *
     * @param subscribedCatalogue The catalogue we want to query
     * @return List<PublishedModel> The list of published models returned by the catalogue
     *
     */
    Object listPublishedModels(SubscribedCatalogue subscribedCatalogue, boolean includeAuthority = false) {
        FederationClient federationClient = getFederationClientForSubscribedCatalogue(subscribedCatalogue)

        switch (subscribedCatalogue.subscribedCatalogueType) {
            case SubscribedCatalogueType.MDM_JSON:
                Map<String, Object> subscribedCatalogueModels = federationClient.withCloseable {client ->
                    client.getSubscribedCatalogueModels(subscribedCatalogue.apiKey)
                }

                Authority subscribedAuthority
                if (includeAuthority) {
                    subscribedAuthority = new Authority(label: subscribedCatalogueModels.authority.label, url: subscribedCatalogueModels.authority.url)
                }

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
                }.sort()

                return [subscribedAuthority, publishedModels]

                break
            case SubscribedCatalogueType.ATOM:
                GPathResult subscribedCatalogueModelsFeed = federationClient.withCloseable {client ->
                    client.getSubscribedCatalogueModelsFromAtomFeed(subscribedCatalogue.apiKey)
                }

                Authority subscribedAuthority
                if (includeAuthority) {
                    subscribedAuthority = new Authority(label: subscribedCatalogueModelsFeed.author.name, url: subscribedCatalogueModelsFeed.author.uri)
                }

                List<PublishedModel> publishedModels = subscribedCatalogueModelsFeed.entry.collect {entry ->
                    new PublishedModel().tap {
                        modelId = Utils.toUuid(extractUuidFromUrn(entry.id))
                        title = entry.title
                        modelType = entry.category.@term
                        lastUpdated = OffsetDateTime.parse(entry.updated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        datePublished = OffsetDateTime.parse(entry.published.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        author = entry.author.name
                        description = entry.summary
                        links = entry.link.collect {link ->
                            new Link(LINK_RELATIONSHIP_ALTERNATE, link.href).tap {contentType = link.@type}
                        }
                    }
                }

                return [subscribedAuthority, publishedModels]

                break
        }
    }

    List<Map<String, Object>> getAvailableExportersForResourceType(SubscribedCatalogue subscribedCatalogue, String urlResourceType) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            client.getAvailableExporters(subscribedCatalogue.apiKey, urlResourceType)
        }
    }

    Map<String, Object> getVersionLinksForModel(SubscribedCatalogue subscribedCatalogue, String urlModelType, UUID modelId) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            client.getVersionLinksForModel(subscribedCatalogue.apiKey, urlModelType, modelId)
        }
    }

    Map<String, Object> getNewerPublishedVersionsForPublishedModel(SubscribedCatalogue subscribedCatalogue, UUID modelId) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            client.getNewerPublishedVersionsForPublishedModel(subscribedCatalogue.apiKey, modelId)
        }
    }

    String getStringResourceExport(SubscribedCatalogue subscribedCatalogue, String urlResourceType, UUID resourceId, Map exporterInfo) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            client.getStringResourceExport(subscribedCatalogue.apiKey, urlResourceType,
                                           resourceId, exporterInfo)
        }
    }

    private FederationClient getFederationClientForSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        httpClientConfiguration.setReadTimeout(Duration.ofMinutes(
            subscribedCatalogue.connectionTimeout ?: grailsApplication.config.getProperty(SubscribedCatalogue.DEFAULT_CONNECTION_TIMEOUT_CONFIG_PROPERTY, Integer)
        ))
        new FederationClient(subscribedCatalogue,
                             httpClientConfiguration,
                             nettyClientSslBuilder,
                             mediaTypeCodecRegistry)
    }

    /**
     * An ID in the atom feed looks like tag:host,MINTED_DATE:uuid
     * So get everything after the last :
     *
     * @param url A tag ID / URL
     * @return A UUID as a string
     */
    private static String extractUuidFromTagUri(String url) {
        final String separator = ':'
        int lastPos = url.lastIndexOf(separator)

        return url.substring(lastPos + 1)
    }

    /**
     * An ID in the atom feed looks like urn:uuid:{model.id}* So get everything after the last :
     *
     * @param url A urn url
     * @return A UUID as a string
     */
    private static String extractUuidFromUrn(String url) {
        final String separator = ':'
        int lastPos = url.lastIndexOf(separator)

        return url.substring(lastPos + 1)
    }

    void anonymise(String createdBy) {
        SubscribedCatalogue.findAllByCreatedBy(createdBy).each { subscribedCatalogue ->
            subscribedCatalogue.createdBy = AnonymousUser.ANONYMOUS_EMAIL_ADDRESS
            subscribedCatalogue.save(validate: false)
        }
    }
}
