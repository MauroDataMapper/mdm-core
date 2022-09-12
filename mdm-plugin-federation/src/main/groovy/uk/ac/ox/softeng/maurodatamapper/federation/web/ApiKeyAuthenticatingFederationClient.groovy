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

import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.ApiKeyAuthenticationCredentials
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.SubscribedCatalogueAuthenticationType

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.netty.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.uri.UriBuilder
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import org.springframework.context.ApplicationContext

import java.util.concurrent.ThreadFactory

/**
 * @since 14/04/2021
 */
@Slf4j
@SuppressFBWarnings(value = 'UPM_UNCALLED_PRIVATE_METHOD', justification = 'Calls to methods with optional params not detected')
class ApiKeyAuthenticatingFederationClient extends FederationClient<ApiKeyAuthenticationCredentials> {

    static final String API_KEY_HEADER = 'apiKey'

    ApiKeyAuthenticatingFederationClient(SubscribedCatalogue subscribedCatalogue, ApplicationContext applicationContext) {
        this(subscribedCatalogue,
             applicationContext.getBean(HttpClientConfiguration),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry)
        )
    }

    ApiKeyAuthenticatingFederationClient(SubscribedCatalogue subscribedCatalogue,
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

    protected ApiKeyAuthenticatingFederationClient(SubscribedCatalogue subscribedCatalogue,
                                                   HttpClientConfiguration httpClientConfiguration,
                                                   ThreadFactory threadFactory,
                                                   NettyClientSslBuilder nettyClientSslBuilder,
                                                   MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        super(subscribedCatalogue,
              httpClientConfiguration,
              threadFactory,
              nettyClientSslBuilder,
              mediaTypeCodecRegistry
        )
    }

    @Override
    boolean handles(SubscribedCatalogueAuthenticationType authenticationType) {
        authenticationType == SubscribedCatalogueAuthenticationType.API_KEY
    }

    @Override
    protected Map<String, Object> retrieveMapFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest
                                             .GET(uriBuilder.expand(params))
                                             .header(API_KEY_HEADER, authenticationCredentials.apiKey.toString()),
                                         Argument.mapOf(String, Object))
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    @Override
    protected String retrieveStringFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest
                                             .GET(uriBuilder.expand(params))
                                             .header(API_KEY_HEADER, authenticationCredentials.apiKey.toString()),
                                         Argument.STRING)
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    @Override
    protected List<Map<String, Object>> retrieveListFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest
                                             .GET(uriBuilder.expand(params))
                                             .header(API_KEY_HEADER, authenticationCredentials.apiKey.toString()),
                                         Argument.listOf(Map<String, Object>)) as List<Map<String, Object>>
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    @Override
    protected byte[] retrieveBytesFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest.GET(uriBuilder.expand(params))
                                             .header(API_KEY_HEADER, authenticationCredentials.apiKey.toString()),
                                         Argument.of(byte[])) as byte[]
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }
}
