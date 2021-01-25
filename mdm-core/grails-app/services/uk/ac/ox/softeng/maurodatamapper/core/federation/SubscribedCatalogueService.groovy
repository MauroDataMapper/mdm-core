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
package uk.ac.ox.softeng.maurodatamapper.core.federation

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional

import groovy.util.logging.Slf4j

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.DefaultHttpClient
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

import org.xml.sax.SAXParseException

import static io.micronaut.http.HttpStatus.OK

@Transactional
@Slf4j
class SubscribedCatalogueService implements XmlImportMapping {

    static final String API_KEY_HEADER = 'apiKey'
    static final String BASE_PATH = 'api'
    static final String FEED_PATH = 'feeds/all'

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

    /**
     * Return a list of models available on the subscribed catalogue. 
     * 1. Connect to the endpoint /api/feeds/all on the remote, authenticating by setting an api key in the header
     * 2. Parse the returned Atom feed, picking out <entry> nodes
     * 3. For each <entry>, create an AvailableModel
     * 4. Return the list of AvailableModel, in order that this can be rendered as json
     *
     * @param subscribedCatalogue The catalogue we want to query
     * @return List<AvailableModel>  The list of available models returned by the catalogue
     *
     */
    List<AvailableModel> listAvailableModels(SubscribedCatalogue subscribedCatalogue) {
        List<AvailableModel> models = []

        String endpoint = makeFeedUrl(subscribedCatalogue.url)
        try {
            HttpResponse response = GET(endpoint, subscribedCatalogue.apiKey)

            log.debug("response.status() {}", response.status())
            //Iterate the <entry> nodes, making an AvailableModel for each one
            if (response.status() == OK) {
                def rootNode = new XmlParser().parseText(response.body())
                def entries = rootNode.children().findAll { it.name().localPart == 'entry' }
                entries.each { entry ->
                    AvailableModel model = new AvailableModel()
                    if (entry.id) {
                        model.id = Utils.toUuid(extractUuidFromUrn(entry.id.text()))
                    }

                    if (entry.title) {
                        model.label = entry.title.text()
                    }

                    if (entry.summary) {
                        model.description = entry.summary.text()
                    }

                    if (entry.category) {
                        if (entry.category.@term) {
                            model.modelType = entry.category.@term[0]
                        }
                    }

                    //When the lastUpdated of the model was written to Atom, we converted it to epoch seconds.
                    //So this gets it back to a OffsetDateTime
                    if (entry.updated) {
                        model.lastUpdated = OffsetDateTime.ofInstant(Instant.parse(entry.updated.text()), ZoneOffset.UTC)
                    }

                    models += model
                }
            } else {
                throw new ApiBadRequestException('SCAT01', "Response from ${endpoint} was ${response.status()}")
            }
        } 
        catch (SAXParseException exception) {
            throw new ApiBadRequestException('SCAT02', 'Could not parse XML returned by endpoint - is the provided URL correct?')
        }

        return models
    }

    /**
     * GET a resource from an endpoint, using an apiKey for authentication
     *
     * @param resourceEndpoint The URL of the endpoint
     * @param apiKey API key to use for authentication
     * @return HttpResponse<String> The response as a String
     *
     */
    HttpResponse<String> GET(String resourceEndpoint, UUID apiKey) {
        log.debug("GET {}", resourceEndpoint)
        exchange(HttpRequest.GET(resourceEndpoint).header(API_KEY_HEADER, apiKey.toString()), Argument.of(String))
    }

    private <B> HttpResponse<B> exchange(MutableHttpRequest request, Argument<B> bodyType) {
        
        HttpClient client = new DefaultHttpClient(request.getUri().toURL(),
                                                  new DefaultHttpClientConfiguration().with {
                                                      setReadTimeout(Duration.ofMinutes(30))
                                                      setReadIdleTimeout(Duration.ofMinutes(30))
                                                      it
                                                  })
        
        try {
            HttpResponse<B> response = client.toBlocking().exchange(request, bodyType)
            response

        } catch (HttpClientResponseException responseException) {
            return responseException.response as HttpResponse<B>
        }
    }    


    /**
     * Make a URL for an endpoint on the subscribed catalogue which will return an Atom feed of available models
     *
     * @param String The root URL of the subscribed catalogue
     * @return a String for the /feeds/all endpoint
     */
    private static String makeFeedUrl(String url) {
        return String.format("%s/%s/%s", url, BASE_PATH, FEED_PATH)
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
     * An ID in the atom feed looks like urn:uuid:{model.id}
     * So get everything after the last :
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
