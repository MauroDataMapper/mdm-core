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
package uk.ac.ox.softeng.maurodatamapper.core.tree

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@Slf4j
class TreeItemInterceptor implements MdmInterceptor {

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    private static final HibernateProxyHandler hibernateProxyHandler = new HibernateProxyHandler()

    @Override
    boolean isShow() {
        actionName in ['show', 'ancestors']
    }

    boolean before() {
        if (params.containerDomainType) {
            mapDomainTypeToClass('container', true)

            if (!params.containerClass) {
                render status: HttpStatus.NOT_FOUND, model: [path: request.requestURI]
                return false
            }

            if (!Utils.parentClassIsAssignableFromChild(Container, params.containerClass)) {
                throw new ApiBadRequestException('TII01', "Tree called for non-Container class ${params.containerDomainType}")
            }

            if (Utils.parentClassIsAssignableFromChild(VersionedFolder, params.containerClass)) {
                throw new ApiBadRequestException('TII02', 'Tree called for VersionedFolder, this is not allowed')
            }

            if (actionName in ['documentationSupersededModels', 'modelSupersededModels', 'deletedModels']) {
                return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: forbiddenDueToNotApplicationAdministrator()
            }

            if (isShow()) {
                if (params.containerId) {
                    return checkActionAuthorisationOnUnsecuredResource(params.containerClass, params.containerId, params.containerClass, params.containerid)
                }
                mapDomainTypeToClass('catalogueItem', true)
                // Verify the user can see the catalogue item to allow them to see the tree
                if (Utils.parentClassIsAssignableFromChild(SecurableResource, params.catalogueItemClass)) {
                    return checkActionAuthorisationOnUnsecuredResource(params.catalogueItemClass, params.catalogueItemId, params.catalogueItemClass,
                                                                       params.catalogueItemId)
                }

                Model model = hibernateProxyHandler.unwrapIfProxy(getOwningModel())
                return checkActionAuthorisationOnUnsecuredResource(params.catalogueItemClass, params.catalogueItemId, model.getClass(), model.getId())
            }
            // Otherwise top level action so should be allowed through
            return true
        } else if (params.modelDomainType) {
            mapDomainTypeToClass('model', true)
            if (!params.modelClass) {
                render status: HttpStatus.NOT_FOUND, model: [path: request.requestURI]
                return false
            }
            if (!Utils.parentClassIsAssignableFromChild(Model, params.modelClass)) {
                throw new ApiBadRequestException('TII02', "Tree called for non-Model class ${params.modelDomainType}")
            }
            if (actionName == 'fullModelTree') {
                boolean canRead = currentUserSecurityPolicyManager.userCanReadResourceId(params.modelClass, params.modelId, params.modelClass,
                                                                                         params.modelId)
                return canRead ?: notFound(params.modelClass, params.modelId)
            } else {
                render status: HttpStatus.NOT_FOUND, model: [path: request.requestURI]
                return false
            }
        }
        throw new ApiBadRequestException('MCI01', 'No domain class resource provided')
    }

    Model getOwningModel() {
        ModelItem modelItem = findModelItemByDomainTypeAndId(params.catalogueItemClass, params.catalogueItemId)
        modelItem.getModel()
    }

    ModelItem findModelItemByDomainTypeAndId(Class domainType, UUID catalogueItemId) {
        ModelItemService service = modelItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('FI01', "TreeItem retrieval for model item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }
}
