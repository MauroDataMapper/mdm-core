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
import uk.ac.ox.softeng.maurodatamapper.federation.converter.SubscribedCatalogueConverter
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
class SubscribedCatalogueService implements AnonymisableService {

    @Autowired
    Set<SubscribedCatalogueConverter> subscribedCatalogueConverters

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
            def (Authority subscribedAuthority, List<PublishedModel> publishedModels) = listPublishedModelsWithAuthority(subscribedCatalogue)

            if (!subscribedAuthority.label || publishedModels == null) {
                subscribedCatalogue.errors.reject('invalid.subscription.url.response',
                                                  [subscribedCatalogue.url].toArray(),
                                                  'Invalid subscription to catalogue at [{0}], response from catalogue is invalid')
            }

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
    List<PublishedModel> listPublishedModels(SubscribedCatalogue subscribedCatalogue) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            getConverterForSubscribedCatalogue(subscribedCatalogue).getPublishedModels(client, subscribedCatalogue)
        }
    }

    Tuple2<Authority, List<PublishedModel>> listPublishedModelsWithAuthority(SubscribedCatalogue subscribedCatalogue) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            getConverterForSubscribedCatalogue(subscribedCatalogue).getAuthorityAndPublishedModels(client, subscribedCatalogue)
        }
    }

    List<PublishedModel> getAuthority(SubscribedCatalogue subscribedCatalogue) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            getConverterForSubscribedCatalogue(subscribedCatalogue).getAuthority(client, subscribedCatalogue)
        }
    }

    /*List<Map<String, Object>> getAvailableExportersForResourceType(SubscribedCatalogue subscribedCatalogue, String urlResourceType) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            client.getAvailableExporters(subscribedCatalogue.apiKey, urlResourceType)
        }
    }*/

    Map<String, Object> getVersionLinksForModel(SubscribedCatalogue subscribedCatalogue, String urlModelType, String publishedModelId) {
        if (subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.MDM_JSON) {
            getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
                client.getVersionLinksForModel(subscribedCatalogue.apiKey, urlModelType, publishedModelId)
            }
        }
    }

    Map<String, Object> getNewerPublishedVersionsForPublishedModel(SubscribedCatalogue subscribedCatalogue, String publishedModelId) {
        if (subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.MDM_JSON) {
            getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
                client.getNewerPublishedVersionsForPublishedModel(subscribedCatalogue.apiKey, publishedModelId)
            }
        }
    }

    /*String getStringResourceExport(SubscribedCatalogue subscribedCatalogue, String urlResourceType, UUID resourceId, Map exporterInfo) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            client.getStringResourceExport(subscribedCatalogue.apiKey, urlResourceType,
                                           resourceId, exporterInfo)
        }
    }*/

    byte[] getBytesResourceExport(SubscribedCatalogue subscribedCatalogue, String resourceUrl) {
        getFederationClientForSubscribedCatalogue(subscribedCatalogue).withCloseable {client ->
            client.getBytesResourceExport(subscribedCatalogue.apiKey, resourceUrl)
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

    private SubscribedCatalogueConverter getConverterForSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        subscribedCatalogueConverters.find {it.handles(subscribedCatalogue.subscribedCatalogueType)}
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
