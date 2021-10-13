/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.federation.web.FederationClient
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.util.logging.Slf4j
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Transactional
@Slf4j
class SubscribedCatalogueService implements XmlImportMapping {

    @Autowired
    HttpClientConfiguration httpClientConfiguration
    @Autowired
    NettyClientSslBuilder nettyClientSslBuilder
    @Autowired
    MediaTypeCodecRegistry mediaTypeCodecRegistry

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
            FederationClient client = getFederationClientForSubscribedCatalogue(subscribedCatalogue)
            Map<String, Object> catalogueModels = client.getSubscribedCatalogueModels(subscribedCatalogue.apiKey)
            if (!catalogueModels) {
                subscribedCatalogue.errors.reject('invalid.subscription.url.apikey',
                                                  [subscribedCatalogue.url].toArray(),
                                                  'Invalid subscription to catalogue at [{0}] cannot connect using provided Api Key')
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
            }
        } catch (Exception exception) {
            subscribedCatalogue.errors.reject('invalid.subscription.url.apikey',
                                              [subscribedCatalogue.url].toArray(),
                                              'Invalid subscription to catalogue at [{0}] cannot connect using provided Api Key')
            log.warn('Unable to confirm catalogue subscription due to exception', exception)
        }
        subscribedCatalogue
    }

    /**
     * Return a list of models available on the subscribed catalogue.
     * 1. Connect to the endpoint /api/published/models on the remote, authenticating by setting an api key in the header
     * 2. Parse the returned Atom feed, picking out <entry> nodes
     * 3. For each <entry>, create an AvailableModel
     * 4. Return the list of AvailableModel, in order that this can be rendered as json
     *
     * @param subscribedCatalogue The catalogue we want to query
     * @return List<AvailableModel>               The list of available models returned by the catalogue
     *
     */
    List<PublishedModel> listPublishedModels(SubscribedCatalogue subscribedCatalogue) {

        FederationClient client = getFederationClientForSubscribedCatalogue(subscribedCatalogue)

        Map<String, Object> subscribedCatalogueModels = client.getSubscribedCatalogueModels(subscribedCatalogue.apiKey)

        if (subscribedCatalogueModels.publishedModels.isEmpty()) return []

        (subscribedCatalogueModels.publishedModels as List<Map<String, String>>).collect {pm ->
            new PublishedModel().tap {
                modelId = Utils.toUuid(pm.modelId)
                title = pm.title
                modelType = pm.modelType
                lastUpdated = OffsetDateTime.parse(pm.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                dateCreated = OffsetDateTime.parse(pm.dateCreated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                datePublished = OffsetDateTime.parse(pm.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                author = pm.author
                description = pm.description
            }
        }
    }

    List<Map<String, Object>> getAvailableExportersForResourceType(SubscribedCatalogue subscribedCatalogue, String urlResourceType) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).getAvailableExporters(subscribedCatalogue.apiKey, urlResourceType)
    }

    Map<String, Object> getVersionLinksForModel(SubscribedCatalogue subscribedCatalogue, String urlModelType, UUID modelId) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).getVersionLinksForModel(subscribedCatalogue.apiKey, urlModelType, modelId)
    }

    String getStringResourceExport(SubscribedCatalogue subscribedCatalogue, String urlResourceType, UUID resourceId, Map exporterInfo) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).getStringResourceExport(subscribedCatalogue.apiKey, urlResourceType,
                                                                                               resourceId, exporterInfo)
    }

    private FederationClient getFederationClientForSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        new FederationClient(subscribedCatalogue.url,
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
        final String separator = ":"
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
        final String separator = ":"
        int lastPos = url.lastIndexOf(separator)

        return url.substring(lastPos + 1)
    }
}
