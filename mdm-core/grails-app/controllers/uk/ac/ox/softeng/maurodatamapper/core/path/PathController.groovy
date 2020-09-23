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
package uk.ac.ox.softeng.maurodatamapper.core.path

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import org.grails.orm.hibernate.proxy.HibernateProxyHandler

import org.springframework.beans.factory.annotation.Autowired

import grails.rest.RestfulController

class PathController extends RestfulController<CatalogueItem> implements MdmController {

    static responseFormats = ['json', 'xml']

    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

    PathService pathService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    PathController() {
        super(CatalogueItem)
    }

    def show() {
        CatalogueItem catalogueItem = pathService.findCatalogueItemByPath(currentUserSecurityPolicyManager, params)
        if (!catalogueItem) return notFound(CatalogueItem, params.path)

        //In order to display availableActions within the view we need to provide a securable resource
        CatalogueItem owningSecurableResource = catalogueItem
        if (catalogueItem instanceof ModelItem) {
            owningSecurableResource = proxyHandler.unwrapIfProxy(catalogueItem.getModel())
        }

        respond(catalogueItem, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager, catalogueItem: catalogueItem, owningSecurableResource: owningSecurableResource], view: 'show'])
    }

}
