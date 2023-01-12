/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.security.test

import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.authentication.AuthenticationSchemeService

import groovy.util.logging.Slf4j

@Slf4j
class TestAuthenticationSchemeService implements AuthenticationSchemeService {

    CatalogueUserService catalogueUserService

    @Override
    String getName() {
        'test'
    }

    @Override
    String getDisplayName() {
        'Test Database Authentication'
    }

    @Override
    CatalogueUser authenticateAndObtainUser(Map<String, Object> authenticationInformation) {

        log.debug("Authenticating user ${authenticationInformation.username} using basic database authentication")

        String username = authenticationInformation.username?.trim()
        String password = authenticationInformation.password?.trim()

        if (!username) return null

        CatalogueUser user = catalogueUserService.findByEmailAddress(username as String)
        if (!user || user.isDisabled()) return null

        if (catalogueUserService.validateTempPassword(user, password)) return user
        if (catalogueUserService.validateUserPassword(user, password)) return user
        null
    }

    @Override
    int getOrder() {
        LOWEST_PRECEDENCE
    }


}
