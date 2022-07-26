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

package uk.ac.ox.softeng.maurodatamapper.federation.authentication

enum SubscribedCatalogueAuthenticationCredentialsType {
    OAUTH_CLIENT_CREDENTIALS('OAuth (Client Credentials)'),
    API_KEY('API Key')

    String label

    SubscribedCatalogueAuthenticationCredentialsType(String label) {
        this.label = label
    }

    static List<String> labels() {
        values().collect {it.label}.sort()
    }

    static SubscribedCatalogueAuthenticationCredentialsType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_').replaceAll(/\(|\)/, '')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static SubscribedCatalogueAuthenticationCredentialsType findFromMap(def map) {
        map['subscribedCatalogueAuthenticationCredentialsType'] instanceof SubscribedCatalogueAuthenticationCredentialsType ?
        map['subscribedCatalogueAuthenticationCredentialsType'] as SubscribedCatalogueAuthenticationCredentialsType :
        findForLabel(map['subscribedCatalogueAuthenticationCredentialsType'] as String)
    }
}