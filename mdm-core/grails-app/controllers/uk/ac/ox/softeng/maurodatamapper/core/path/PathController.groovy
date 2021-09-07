/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.path

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.DomainClass
import grails.rest.RestfulController
import org.springframework.beans.factory.annotation.Autowired

class PathController extends RestfulController<CatalogueItem> implements MdmController {

    static responseFormats = ['json', 'xml']

    PathService pathService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    PathController() {
        super(CatalogueItem)
    }

    def show() {
        MdmDomain pathedResource
        if (params.securableResourceId) {
            SecurableResource resource = pathService.findSecurableResourceByDomainClassAndId(params.securableResourceClass,
                                                                                             params.securableResourceId)

            if (!resource) {
                return notFound(params.securableResourceClass, params.securableResourceId)
            }

            if (!(resource instanceof MdmDomain)) {
                throw new ApiBadRequestException('PC01', "[${params.securableResourceDomainType}] is not a pathable resource")
            }

            // Permissions have been checked as part of the interceptor
            pathedResource = pathService.findResourceByPathFromRootResource(resource as MdmDomain, params.path)
        } else {
            pathedResource = pathService.findResourceByPathFromRootClass(params.securableResourceClass, params.path, currentUserSecurityPolicyManager)
        }

        if (!pathedResource) return notFound(DomainClass, params.path)

        respond(pathedResource, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager,
                                         pathedResource           : pathedResource],
                                 view : 'show'])
    }

    def listAllPrefixMappings() {
        respond pathService.listAllPrefixMappings()

    }
}
