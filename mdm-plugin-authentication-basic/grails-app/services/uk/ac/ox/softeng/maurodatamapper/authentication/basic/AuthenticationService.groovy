/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.authentication.basic

import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.authentication.AuthenticationSchemeService

import grails.gorm.transactions.Transactional

@Transactional
class AuthenticationService implements AuthenticationSchemeService {

    CatalogueUserService catalogueUserService

    @Override
    String getName() {
        'basic'
    }

    @Override
    String getDisplayName() {
        'Basic Database Authentication'
    }

    @Override
    CatalogueUser authenticateAndObtainUser(String emailAddress, String password) {

        log.debug("Authenticating user ${emailAddress} using basic database authentication")
        if (!emailAddress) return null
        CatalogueUser user = catalogueUserService.findByEmailAddress(emailAddress)
        if (!user || user.isDisabled()) return null

        if (catalogueUserService.validateTempPassword(user, password?.trim())) return user
        if (catalogueUserService.validateUserPassword(user, password?.trim())) return user
        null
    }

    @Override
    int getOrder() {
        LOWEST_PRECEDENCE
    }


}
