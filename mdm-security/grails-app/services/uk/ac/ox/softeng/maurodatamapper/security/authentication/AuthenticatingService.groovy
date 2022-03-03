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
package uk.ac.ox.softeng.maurodatamapper.security.authentication


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpSession

@Slf4j
class AuthenticatingService {

    @Autowired(required = false)
    List<AuthenticationSchemeService> authenticationSchemeServices

    SessionService sessionService
    CatalogueUserService catalogueUserService
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService

    @Transactional
    CatalogueUser authenticateAndObtainUser(Map<String, Object> authenticationInformation, String scheme = null) {

        if (scheme) {
            log.debug('Attempting to authenticate user via schema {}', scheme)
            AuthenticationSchemeService service = authenticationSchemeServices.find {it.name == scheme}
            if (!service) {
                throw new ApiBadRequestException('AS01', "No authentication scheme found for ${scheme}")
            }
            return service.authenticateAndObtainUser(authenticationInformation)
        }

        if (!authenticationSchemeServices) {
            throw new ApiInternalException('ASXX', 'No authentication scheme services available')
        }

        log.debug('Trying {} authentication schemes to authenticate user', authenticationSchemeServices.size())
        for (AuthenticationSchemeService service : authenticationSchemeServices.sort {it.order}) {
            CatalogueUser catalogueUser = service.authenticateAndObtainUser(authenticationInformation)
            if (catalogueUser) return catalogueUser
        }
        null
    }

    UserSecurityPolicyManager buildUserSecurityPolicyManager(CatalogueUser catalogueUser) {
        groupBasedSecurityPolicyManagerService.asyncBuildNewUserSecurityPolicyManager(catalogueUser)
    }

    @Transactional
    void registerUserAsLoggedIn(CatalogueUser catalogueUser, HttpSession httpSession) {
        catalogueUserService.setUserLastLoggedIn(catalogueUser)
        sessionService.setUserEmailAddress(httpSession, catalogueUser.emailAddress)
        sessionService.setUserName(httpSession,catalogueUser.firstName, catalogueUser.lastName)
        sessionService.setUserOrganisation(httpSession,catalogueUser.organisation)
        log.debug("${httpSession.id}:${catalogueUser.emailAddress} logged in")
    }

    void registerUserAsLoggedOut(HttpSession httpSession) {
        groupBasedSecurityPolicyManagerService.removeUserSecurityPolicyManager(sessionService.getSessionEmailAddress(httpSession))
        sessionService.destroySession(httpSession)
    }

    boolean isAuthenticatedSession(HttpSession httpSession) {
        sessionService.isAuthenticatedSession(httpSession.id)
    }

    String getEmailAddressForSession(HttpSession httpSession) {
        sessionService.getSessionEmailAddress(httpSession)
    }
}
