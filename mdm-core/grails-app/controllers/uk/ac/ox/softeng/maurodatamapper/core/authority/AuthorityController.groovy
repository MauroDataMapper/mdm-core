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
package uk.ac.ox.softeng.maurodatamapper.core.authority

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class AuthorityController extends EditLoggingController<Authority> {

    static responseFormats = ['json', 'xml']

    AuthorityService authorityService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    AuthorityController() {
        super(Authority)
    }

    @Override
    protected Authority saveResource(Authority resource) {
        Authority authority = super.saveResource(resource) as Authority
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(resource, currentUser, resource.label)
        }
        authority
    }

    protected Authority updateResource(Authority resource) {
        Set<String> changedProperties = resource.getDirtyPropertyNames()
        Authority authority = super.updateResource(resource) as Authority
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(authority,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }
        authority
    }

    @Override
    protected Authority queryForResource(Serializable id) {
        authorityService.get(id)
    }

    @Override
    protected List<Authority> listAllReadableResources(Map params) {
        authorityService.list()
    }

    @Override
    void serviceDeleteResource(Authority resource) {
        authorityService.delete(resource)
    }

    @Override
    protected boolean validateResourceDeletion(Authority instance, String view) {
        if (instance.defaultAuthority) {
            return forbidden('Cannot delete the default authority')
        }
        true
    }
}
