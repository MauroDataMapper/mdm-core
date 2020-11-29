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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.beans.factory.annotation.Autowired

class ProfileInterceptor implements MdmInterceptor {

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    protected static HibernateProxyHandler proxyHandler = new HibernateProxyHandler()

    void resourceChecks() {
        Utils.toUuid(params, 'id')
        mapDomainTypeToClass('catalogueItem')
    }

    boolean before() {
        resourceChecks()
        if (actionName in ['getProfile', 'setProfile']) {
            return checkActionAllowedOnCatalogueItem()
        }
        true
    }

    boolean checkActionAllowedOnCatalogueItem() {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, params.catalogueItemClass)) {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(params.catalogueItemClass, params.catalogueItemId) ?:
                   notFound(params.catalogueItemClass, params.catalogueItemId)
        }

        Model model = proxyHandler.unwrapIfProxy(getOwningModel()) as Model
        currentUserSecurityPolicyManager.userCanReadResourceId(params.catalogueItemClass, params.id, model.getClass(), model.getId()) ?:
        notFound(params.catalogueItemClass, params.catalogueItemId)
    }

    Model getOwningModel() {
        ModelItem modelItem = findModelItemByDomainTypeAndId(params.catalogueItemClass, params.catalogueItemId)
        modelItem.getModel()
    }

    ModelItem findModelItemByDomainTypeAndId(Class domainType, UUID catalogueItemId) {
        ModelItemService service = modelItemServices.find { it.handles(domainType) }
        if (!service) throw new ApiBadRequestException('FI01', "Facet retrieval for model item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }
}
