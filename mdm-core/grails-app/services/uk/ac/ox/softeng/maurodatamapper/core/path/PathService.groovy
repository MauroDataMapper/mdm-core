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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.util.Path
import uk.ac.ox.softeng.maurodatamapper.util.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import org.grails.orm.hibernate.proxy.HibernateProxyHandler

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class PathService {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

    CatalogueItem findCatalogueItemByPath(UserSecurityPolicyManager userSecurityPolicyManager, Map params) {
        Path path = new Path(params.path)
        CatalogueItemService service = catalogueItemServices.find { it.handles(params.catalogueItemDomainType) }
        CatalogueItem catalogueItem

        /*
        Iterate over nodes in the path
         */
        boolean first = true
        path.pathNodes.each { PathNode node ->
            /*
            On first iteration, if params.catalogueItemId is provided then use this to get the top CatalogueItem by ID.
            Else if the service handles the typePrefix then use this service to find the top CatalogueItem by label.
            */
            if (first) {
                if (params.catalogueItemId) {
                    catalogueItem = service.get(params.catalogueItemId)
                } else {
                    if (service.handlesPathPrefix(node.typePrefix) && node.label) {
                        catalogueItem = service.findByLabel(node.label)
                    }
                }

                /*
                Only return anything if the first item retrieved is a model which is securable and readable, or it belongs to a model which is securable and readable
                 */
                boolean readable = false
                if (catalogueItem instanceof Model) {
                    readable = userSecurityPolicyManager.userCanReadSecuredResourceId(catalogueItem.getClass(), catalogueItem.id)
                } else if (catalogueItem instanceof ModelItem) {
                    CatalogueItem model = proxyHandler.unwrapIfProxy(catalogueItem.getModel())
                    readable = userSecurityPolicyManager.userCanReadResourceId(catalogueItem.getClass(), catalogueItem.id, model.getClass(), model.id)
                }

                if (!readable) {
                    catalogueItem = null
                }


                first = false
            } else {
                if (catalogueItem) {
                    //Try to find the child of this catalogue item by prefix and label
                    service = catalogueItemServices.find { it.handlesPathPrefix(node.typePrefix) }

                    //Use the service to find a child CatalogueItem whose parent is catalogueItem and which has the specified label
                    //Missing method exception means the path tried to retrieve a type of parent that is not expected
                    try {
                        catalogueItem = service.findByParentAndLabel(catalogueItem, node.label)
                    } catch (groovy.lang.MissingMethodException ex) {
                        catalogueItem = null
                    }

                }
            }
        }

        catalogueItem
    }

}
