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
package uk.ac.ox.softeng.maurodatamapper.core.interceptor

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 07/02/2020
 */
abstract class FacetInterceptor implements MdmInterceptor {

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    protected static final HibernateProxyHandler hibernateProxyHandler = new HibernateProxyHandler();

    abstract Class getFacetClass()

    String getOwningType() {
        'multiFacetAwareItem'
    }

    Class getOwningClass() {
        String key = getOwningType() + 'Class'
        params[key]
    }

    UUID getOwningId() {
        String key = getOwningType() + 'Id'
        params[key]
    }

    void checkAdditionalIds() {
        // No-op
    }

    void checkParentId() throws ApiBadRequestException {
        // no op
    }

    void facetResourceChecks() {
        Utils.toUuid(params, 'id')
        params.multiFacetAwareItemDomainType = params.multiFacetAwareItemDomainType ?: params.catalogueItemDomainType ?: params.containerDomainType
        params.multiFacetAwareItemId = params.multiFacetAwareItemId ?: params.catalogueItemId ?: params.containerId
        checkAdditionalIds()
        mapDomainTypeToClass(getOwningType(), true)
        checkParentId()
    }

    boolean checkActionAllowedOnFacet() {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, getOwningClass())) {
            return checkActionAuthorisationOnUnsecuredResource(getFacetClass(), params.id, getOwningClass(), getOwningId())
        }

        Model model = hibernateProxyHandler.unwrapIfProxy(getOwningModel())
        return checkActionAuthorisationOnUnsecuredResource(getFacetClass(), params.id, model.getClass(), model.getId())
    }

    Model getOwningModel() {
        ModelItem modelItem = findModelItemByDomainTypeAndId(getOwningClass(), getOwningId())
        modelItem.getModel()
    }

    ModelItem findModelItemByDomainTypeAndId(Class domainType, UUID catalogueItemId) {
        ModelItemService service = modelItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('FI01', "Facet retrieval for model item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }
}