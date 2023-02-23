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

package uk.ac.ox.softeng.maurodatamapper.federation.authentication

import uk.ac.ox.softeng.maurodatamapper.federation.web.ApiKeyAuthenticatingFederationClient
import uk.ac.ox.softeng.maurodatamapper.federation.web.FederationClient
import uk.ac.ox.softeng.maurodatamapper.federation.web.OAuthAuthenticatingFederationClient

import groovy.util.logging.Slf4j

@Slf4j
enum SubscribedCatalogueAuthenticationType {
    OAUTH_CLIENT_CREDENTIALS('OAuth (Client Credentials)'),
    API_KEY('API Key'),
    NO_AUTHENTICATION('No Authentication')

    String label

    SubscribedCatalogueAuthenticationType(String label) {
        this.label = label
    }

    static List<String> labels() {
        values().collect {it.label}.sort()
    }

    static SubscribedCatalogueAuthenticationType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_').replaceAll(/\(|\)/, '')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static SubscribedCatalogueAuthenticationType findFromMap(def map) {
        map['subscribedCatalogueAuthenticationType'] instanceof SubscribedCatalogueAuthenticationType ?
        map['subscribedCatalogueAuthenticationType'] as SubscribedCatalogueAuthenticationType :
        findForLabel(map['subscribedCatalogueAuthenticationType'] as String)
    }

    static Class findDomainClassFromType(SubscribedCatalogueAuthenticationType type) {
        switch (type) {
            case OAUTH_CLIENT_CREDENTIALS:
                return OAuthClientCredentialsAuthenticationCredentials
            case API_KEY:
                return ApiKeyAuthenticationCredentials
            case NO_AUTHENTICATION:
                return null
            default:
                log.warn('Unknown authentication credentials type [{}]', type)
                return null
        }
    }

    static Class findFederationClientClassFromType(SubscribedCatalogueAuthenticationType type) {
        switch (type) {
            case OAUTH_CLIENT_CREDENTIALS:
                return OAuthAuthenticatingFederationClient
            case API_KEY:
                return ApiKeyAuthenticatingFederationClient
            case NO_AUTHENTICATION:
                return FederationClient<Void>
            default:
                log.warn('Unknown authentication credentials type [{}]', type)
                return FederationClient<Void>
        }
    }
}