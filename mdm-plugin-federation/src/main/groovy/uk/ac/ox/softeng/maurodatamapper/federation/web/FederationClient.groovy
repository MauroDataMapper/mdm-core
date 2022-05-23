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
package uk.ac.ox.softeng.maurodatamapper.federation.web

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueType

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.core.annotation.AnnotationMetadataResolver
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.LoadBalancer
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.netty.DefaultHttpClient
import io.micronaut.http.client.netty.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.uri.DefaultUriBuilder
import io.micronaut.http.uri.UriBuilder
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import org.springframework.context.ApplicationContext
import org.xml.sax.SAXException

import java.util.concurrent.ThreadFactory
/**
 * @since 14/04/2021
 */
@Slf4j
@SuppressFBWarnings(value = 'UPM_UNCALLED_PRIVATE_METHOD', justification = 'Calls to methods with optional params not detected')
class FederationClient implements Closeable {

    static final String API_KEY_HEADER = 'apiKey'
    private HttpClient client
    private String hostUrl
    private String contextPath
    private HttpClientConfiguration httpClientConfiguration
    private NettyClientSslBuilder nettyClientSslBuilder
    private MediaTypeCodecRegistry mediaTypeCodecRegistry

    FederationClient(SubscribedCatalogue subscribedCatalogue, ApplicationContext applicationContext) {
        this(subscribedCatalogue,
             applicationContext.getBean(HttpClientConfiguration),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry)
        )
    }

    FederationClient(SubscribedCatalogue subscribedCatalogue,
                     HttpClientConfiguration httpClientConfiguration,
                     NettyClientSslBuilder nettyClientSslBuilder,
                     MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        this(subscribedCatalogue,
             httpClientConfiguration,
             new DefaultThreadFactory(MultithreadEventLoopGroup),
             nettyClientSslBuilder,
             mediaTypeCodecRegistry
        )
    }

    private FederationClient(SubscribedCatalogue subscribedCatalogue,
                             HttpClientConfiguration httpClientConfiguration,
                             ThreadFactory threadFactory,
                             NettyClientSslBuilder nettyClientSslBuilder,
                             MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        this.httpClientConfiguration = httpClientConfiguration
        this.nettyClientSslBuilder = nettyClientSslBuilder
        this.mediaTypeCodecRegistry = mediaTypeCodecRegistry

        hostUrl = subscribedCatalogue.url
        // The http client resolves using URI.resolve which ignores anything in the url path,
        // therefore we need to make sure its part of the context path.
        URI hostUri = hostUrl.toURI()
        if (subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.MAURO_JSON) {
            String path = hostUri.path.endsWith('/') ? hostUri.path : "${hostUri.path}/"
            if (!path.endsWith('/api/')) path = path + 'api/'
            this.contextPath = path
        } else {
            this.contextPath = hostUri.path
        }

        client = new DefaultHttpClient(LoadBalancer.fixed(hostUrl.toURL().toURI()),
                                       httpClientConfiguration,
                                       this.contextPath,
                                       threadFactory,
                                       nettyClientSslBuilder,
                                       mediaTypeCodecRegistry,
                                       AnnotationMetadataResolver.DEFAULT,
                                       Collections.emptyList())
        log.debug('Client created to connect to {}', hostUrl)
    }

    @Override
    void close() throws IOException {
        client.close()
    }

    GPathResult getSubscribedCatalogueModelsFromAtomFeed(UUID apiKey) {
        // Currently we use the ATOM feed which is XML and the micronaut client isnt designed to decode XML
        retrieveXmlDataFromClient(UriBuilder.of(''), apiKey)
    }

    Map<String, Object> getSubscribedCatalogueModels(UUID apiKey) {
        retrieveMapFromClient(UriBuilder.of('published/models'), apiKey)
    }

    List<Map<String, Object>> getAvailableExporters(UUID apiKey, String urlResourceType) {
        retrieveListFromClient(UriBuilder.of(urlResourceType).path('providers/exporters'), apiKey)
    }

    Map<String, Object> getVersionLinksForModel(UUID apiKey, String urlModelResourceType, String publishedModelId) {
        retrieveMapFromClient(UriBuilder.of(urlModelResourceType).path(publishedModelId).path('versionLinks'), apiKey)
    }

    Map<String, Object> getNewerPublishedVersionsForPublishedModel(UUID apiKey, String publishedModelId) {
        retrieveMapFromClient(UriBuilder.of('published/models').path(publishedModelId).path('newerVersions'), apiKey)
    }

    byte[] getBytesResourceExport(UUID apiKey, String resourceUrl) {
        retrieveBytesFromClient(UriBuilder.of(resourceUrl), apiKey)
    }

    private GPathResult retrieveXmlDataFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        String body = retrieveStringFromClient(uriBuilder, apiKey, params)
        try {
            new XmlSlurper().parseText(body)
        } catch (IOException | SAXException exception) {
            throw new ApiInternalException('FED01', "Could not translate XML from endpoint [${getFullUrl(uriBuilder, params)}].\n" +
                                                    "Exception: ${exception.getMessage()}")
        }
    }

    private Map<String, Object> retrieveMapFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest
                                             .GET(uriBuilder.expand(params))
                                             .header(API_KEY_HEADER, apiKey.toString()),
                                         Argument.mapOf(String, Object))
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    private String retrieveStringFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest
                                             .GET(uriBuilder.expand(params))
                                             .header(API_KEY_HEADER, apiKey.toString()),
                                         Argument.STRING)
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    private List<Map<String, Object>> retrieveListFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest
                                             .GET(uriBuilder.expand(params))
                                             .header(API_KEY_HEADER, apiKey.toString()),
                                         Argument.listOf(Map<String, Object>)) as List<Map<String, Object>>
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    private byte[] retrieveBytesFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest.GET(uriBuilder.expand(params))
                                             .header(API_KEY_HEADER, apiKey.toString()),
                                         Argument.of(byte[])) as byte[]
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    private static void handleHttpException(HttpException ex, String fullUrl) throws ApiException {
        if (ex instanceof HttpClientResponseException) {
            if (ex.status == HttpStatus.NOT_FOUND) {
                throw new ApiBadRequestException('FED02', "Requested endpoint could not be found ${fullUrl}")
            } else {
                throw new ApiBadRequestException('FED03', "Could not load resource from endpoint [${fullUrl}].\n" +
                                                          "Response body [${ex.response.body()}]",
                                                 ex)
            }
        } else if (ex instanceof HttpClientException) {
            throw new ApiBadRequestException('FED04', "Could not load resource from endpoint [${fullUrl}]", ex)
        }
        throw new ApiInternalException('FED05', "Could not load resource from endpoint [${fullUrl}]", ex)
    }

    private String getFullUrl(UriBuilder uriBuilder, Map params) {
        String path = uriBuilder.expand(params).toString()
        hostUrl.toURI().resolve(UriBuilder.of(contextPath).path(path).build()).toString()
    }
}
