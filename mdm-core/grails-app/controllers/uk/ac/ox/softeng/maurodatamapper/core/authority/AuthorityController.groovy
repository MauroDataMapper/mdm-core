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
package uk.ac.ox.softeng.maurodatamapper.core.authority


import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.NO_CONTENT
class AuthorityController extends EditLoggingController<Authority> /* implements RestResponder */ {
    static responseFormats = ['json', 'xml']

    static allowedMethods = [
    ]

    AuthorityService authorityService

    ApiPropertyService apiPropertyService

    AuthorityController() {
        super(Authority)
    }

    @Override
    protected Authority createResource() {
        Authority instance = super.createResource() as Authority
        instance
    }

    @Override
    protected Authority queryForResource(Serializable id) {
        authorityService.get(id)
    }

    @Override
    protected List<Authority> listAllReadableResources(Map params) {
        if (params.containsKey('authorityId')) {
            return authorityService.findAllByAuthority(Utils.toUuid(params.authorityId), params)
        }
        authorityService.findAllByAuthority(params)
    }

    @Override
    void serviceDeleteResource(Authority resource) {
        authorityService.delete(resource)
    }

    @Override
    protected Authority saveResource(Authority resource) {
        Authority authority = super.saveResource(resource)
        authority
    }

}
