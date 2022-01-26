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
package uk.ac.ox.softeng.maurodatamapper.authentication.apikey

import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKey
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKeyService
import uk.ac.ox.softeng.maurodatamapper.security.authentication.AuthenticatingService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import javax.servlet.http.HttpSession

@Slf4j
class ApiKeyAuthenticatingService extends AuthenticatingService {

    ApiKeyService apiKeyService

    @Transactional
    CatalogueUser authenticateAndObtainUserUsingApiKey(String apiKeyString) {
        log.info('Attempt to access system using API Key')

        ApiKey apiKey = apiKeyService.get(Utils.toUuid(apiKeyString))

        if (!apiKey) {
            log.warn('Attempt to authenticate using unknown API key')
            return null
        }

        CatalogueUser catalogueUser = apiKey.catalogueUser

        if (apiKey.expired) {
            log.warn('Attempt to authenticate using expired API key for user {}', catalogueUser.emailAddress)
            return null
        }
        if (apiKey.disabled) {
            log.warn('Attempt to authenticate using disabled API key for user {}', catalogueUser.emailAddress)
            return null
        }

        catalogueUser
    }

    UserSecurityPolicyManager retrieveOrBuildUserSecurityPolicyManager(CatalogueUser authenticatedUser, HttpSession httpSession) {
        UserSecurityPolicyManager userSecurityPolicyManager =
            groupBasedSecurityPolicyManagerService.retrieveUserSecurityPolicyManager(authenticatedUser.emailAddress)

        // If user hasnt actually authenticated yet then there'll be no policy manager so we should now build it
        if (!userSecurityPolicyManager) {
            userSecurityPolicyManager = buildUserSecurityPolicyManager(authenticatedUser)
            // Also the log the user as having logged in
            registerUserAsLoggedIn(authenticatedUser, httpSession)
        }

        userSecurityPolicyManager
    }

}
