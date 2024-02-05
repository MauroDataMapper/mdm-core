/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.OAuthClientCredentialsAuthenticationCredentials
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.SubscribedCatalogueAuthenticationType

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.http.client.netty.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.uri.UriBuilder
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import org.springframework.context.ApplicationContext

import java.time.OffsetDateTime
import java.util.concurrent.ThreadFactory
/**
 * @since 14/04/2021
 */
@Slf4j
@SuppressFBWarnings(value = 'UPM_UNCALLED_PRIVATE_METHOD', justification = 'Calls to methods with optional params not detected')
class OAuthAuthenticatingFederationClient extends FederationClient<OAuthClientCredentialsAuthenticationCredentials> {

    static final String OAUTH_TOKEN_HEADER = 'Authorization'
    static final Integer OAUTH_TOKEN_EXPIRY_GRACE_SECONDS = 5

    OAuthAuthenticatingFederationClient(SubscribedCatalogue subscribedCatalogue, ApplicationContext applicationContext) {
        this(subscribedCatalogue,
             applicationContext.getBean(HttpClientConfiguration),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry)
        )
    }

    OAuthAuthenticatingFederationClient(SubscribedCatalogue subscribedCatalogue,
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

    protected OAuthAuthenticatingFederationClient(SubscribedCatalogue subscribedCatalogue,
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

        authenticationCredentials = subscribedCatalogue.subscribedCatalogueAuthenticationCredentials
    }

    @Override
    boolean handles(SubscribedCatalogueAuthenticationType authenticationType) {
        authenticationType == SubscribedCatalogueAuthenticationType.OAUTH_CLIENT_CREDENTIALS
    }

    @Override
    protected Map<String, Object> retrieveMapFromClient(UriBuilder uriBuilder, Map params = [:]) {
        try {
            client.toBlocking().retrieve(HttpRequest
                                             .GET(uriBuilder.expand(params))
                                             .header(OAUTH_TOKEN_HEADER, getOAuthBearerToken()),
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
                                             .header(OAUTH_TOKEN_HEADER, getOAuthBearerToken()),
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
                                             .header(OAUTH_TOKEN_HEADER, getOAuthBearerToken()),
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
                                             .header(OAUTH_TOKEN_HEADER, getOAuthBearerToken()),
                                         Argument.of(byte[])) as byte[]
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    private Map<String, String> oAuthTokenRequestBody() {
        [
            grant_type   : 'client_credentials',
            client_id    : authenticationCredentials.clientId,
            client_secret: authenticationCredentials.clientSecret
        ]
    }

    private String getOAuthBearerToken() {
        OffsetDateTime expiryTime = OffsetDateTime.now().plusSeconds(OAUTH_TOKEN_EXPIRY_GRACE_SECONDS)

        if (authenticationCredentials.accessToken && authenticationCredentials.accessTokenExpiryTime > expiryTime) {
            log.debug('OAuth token is still valid, using it.')
        } else {
            log.debug('OAuth token is not present or expired, retrieving a new token.')
            retrieveOAuthTokenFromClient()
        }

        'Bearer ' + authenticationCredentials.accessToken
    }

    private retrieveOAuthTokenFromClient() {
        try {
            Map<String, Object> tokenResponse = client.toBlocking().retrieve(HttpRequest.POST(authenticationCredentials.tokenUrl,
                                                                                              oAuthTokenRequestBody()).contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE),
                                                                             Argument.mapOf(String, Object))

            if (!'bearer'.equalsIgnoreCase(tokenResponse?.token_type)) {
                throw new HttpClientException('OAuth token type must be "bearer"')
            }

            OffsetDateTime expiryTime = OffsetDateTime.now().plusSeconds((Integer) tokenResponse.expires_in)

            if (tokenResponse.access_token && expiryTime) {
                authenticationCredentials.accessToken = tokenResponse.access_token
                authenticationCredentials.accessTokenExpiryTime = expiryTime
            }
        }
        catch (HttpException ex) {
            handleHttpException(ex, authenticationCredentials.tokenUrl)
        }
    }
}
