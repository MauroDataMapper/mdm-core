/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.SubscribedCatalogueAuthenticationCredentials
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.SubscribedCatalogueAuthenticationType

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
class FederationClient<C extends SubscribedCatalogueAuthenticationCredentials> implements Closeable {

    protected HttpClient client
    protected String hostUrl
    protected String contextPath
    protected C authenticationCredentials
    protected HttpClientConfiguration httpClientConfiguration
    protected NettyClientSslBuilder nettyClientSslBuilder
    protected MediaTypeCodecRegistry mediaTypeCodecRegistry

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

    protected FederationClient(SubscribedCatalogue subscribedCatalogue,
                               HttpClientConfiguration httpClientConfiguration,
                               ThreadFactory threadFactory,
                               NettyClientSslBuilder nettyClientSslBuilder,
                               MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        this.httpClientConfiguration = httpClientConfiguration
        this.nettyClientSslBuilder = nettyClientSslBuilder
        this.mediaTypeCodecRegistry = mediaTypeCodecRegistry

        hostUrl = subscribedCatalogue.url
        authenticationCredentials = subscribedCatalogue.subscribedCatalogueAuthenticationCredentials
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

    boolean handles(SubscribedCatalogueAuthenticationType authenticationType) {
        authenticationType == SubscribedCatalogueAuthenticationType.NO_AUTHENTICATION
    }

    @Override
    void close() throws IOException {
        client.close()
    }

    GPathResult getSubscribedCatalogueModelsFromAtomFeed() {
        // Currently we use the ATOM feed which is XML and the micronaut client isnt designed to decode XML
        retrieveXmlDataFromClient(UriBuilder.of(''))
    }

    Map<String, Object> getSubscribedCatalogueModels() {
        retrieveMapFromClient(UriBuilder.of('published/models'))
    }

    List<Map<String, Object>> getAvailableExporters(String urlResourceType) {
        retrieveListFromClient(UriBuilder.of(urlResourceType).path('providers/exporters'))
    }

    Map<String, Object> getVersionLinksForModel(String urlModelResourceType, String publishedModelId) {
        retrieveMapFromClient(UriBuilder.of(urlModelResourceType).path(publishedModelId).path('versionLinks'))
    }

    Map<String, Object> getNewerPublishedVersionsForPublishedModel(String publishedModelId) {
        retrieveMapFromClient(UriBuilder.of('published/models').path(publishedModelId).path('newerVersions'))
    }

    byte[] getBytesResourceExport(String resourceUrl) {
        retrieveBytesFromClient(UriBuilder.of(resourceUrl))
    }

    private GPathResult retrieveXmlDataFromClient(UriBuilder uriBuilder, Map params = [:]) {
        String body = retrieveStringFromClient(uriBuilder, params)
        try {
            new XmlSlurper().parseText(body)
        } catch (IOException | SAXException exception) {
            throw new ApiInternalException('FED01', "Could not translate XML from endpoint [${getFullUrl(uriBuilder, params)}].\n" +
                                                    "Exception: ${exception.getMessage()}")
        }
    }

    protected Map<String, Object> retrieveMapFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest.GET(uriBuilder.expand(params)),
                                         Argument.mapOf(String, Object))
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    protected String retrieveStringFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest.GET(uriBuilder.expand(params)),
                                         Argument.STRING)
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    protected List<Map<String, Object>> retrieveListFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest.GET(uriBuilder.expand(params)),
                                         Argument.listOf(Map<String, Object>)) as List<Map<String, Object>>
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    protected byte[] retrieveBytesFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest.GET(uriBuilder.expand(params)),
                                         Argument.of(byte[])) as byte[]
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    protected static void handleHttpException(HttpException ex, String fullUrl) throws ApiException {
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

    protected String getFullUrl(UriBuilder uriBuilder, Map params) {
        URI requestUri = uriBuilder.expand(params)
        if (requestUri.scheme && requestUri.authority) {
            return requestUri.toString()
        } else {
            String path = requestUri.toString()
            URI hostUri = hostUrl.toURI()
            UriBuilder.of(new URI(hostUri.scheme, hostUri.authority, null, null, null)).path(contextPath).path(path).build().toString()
        }
    }
}
