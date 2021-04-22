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


import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.federation.web.FederationClient
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime

@Transactional
@Slf4j
class SubscribedCatalogueService implements XmlImportMapping {

    @Autowired
    HttpClientConfiguration httpClientConfiguration
    @Autowired
    NettyClientSslBuilder nettyClientSslBuilder
    @Autowired
    MediaTypeCodecRegistry mediaTypeCodecRegistry

    SubscribedCatalogue get(Serializable id) {
        SubscribedCatalogue.get(id)
    }

    List<SubscribedCatalogue> list(Map pagination) {
        pagination ? SubscribedCatalogue.list(pagination) : SubscribedCatalogue.list()
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

    boolean verifyConnectionToSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        FederationClient client = getFederationClientForSubscribedCatalogue(subscribedCatalogue)
        client.isConnectionPossible(subscribedCatalogue.apiKey)
    }

    /**
     * Return a list of models available on the subscribed catalogue. 
     * 1. Connect to the endpoint /api/feeds/all on the remote, authenticating by setting an api key in the header
     * 2. Parse the returned Atom feed, picking out <entry> nodes
     * 3. For each <entry>, create an AvailableModel
     * 4. Return the list of AvailableModel, in order that this can be rendered as json
     *
     * @param subscribedCatalogue The catalogue we want to query
     * @return List<AvailableModel>        The list of available models returned by the catalogue
     *
     */
    List<AvailableModel> listAvailableModels(SubscribedCatalogue subscribedCatalogue) {

        FederationClient client = getFederationClientForSubscribedCatalogue(subscribedCatalogue)

        GPathResult feedData = client.getSubscribedCatalogueModels(subscribedCatalogue.apiKey)

        //Iterate the <entry> nodes, making an AvailableModel for each one
        def entries = feedData.'*'.findAll {node -> node.name() == 'entry'}

        if (entries.isEmpty()) return []

        entries.collect {entry ->
            new AvailableModel(
                id: Utils.toUuid(extractUuidFromUrn(entry.id.text())),
                label: entry.title.text(),
                description: entry.summary.text(),
                modelType: entry.category.@term,
                lastUpdated: OffsetDateTime.parse(entry.updated.text())
            )
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
