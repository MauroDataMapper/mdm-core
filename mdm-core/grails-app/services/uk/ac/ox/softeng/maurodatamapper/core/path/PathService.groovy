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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
//import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Path
import uk.ac.ox.softeng.maurodatamapper.util.PathNode

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class PathService {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    //SessionFactory sessionFactory

    CatalogueItem findCatalogueItemByPath(Map params) {
        log.debug("PathService.findCatalogueItemByPath: ${params.path} ${params.catalogueItemDomainType} ${params.catalogueItemId}")
        Path path = new Path(params.path)
        CatalogueItemService service = catalogueItemServices.find { it.handles(params.catalogueItemDomainType) }

        CatalogueItem catalogueItem

        /*
        Iterate over nodes in the path
         */
        boolean first = true
        path.pathNodes.each {node ->
            /*
            On first iteration, if params.catalogueItemId is provided then use this to get the top CatalogueItem by ID.
            Else if the service handles the typePrefix then use this service to find the top CatalogueItem by label.
             */
            if (first) {
                if (params.catalogueItemId) {
                    catalogueItem = service.get(params.catalogueItemId)
                } else {
                    if (service.handlesPathPrefix(it.typePrefix) && it.label) {
                        catalogueItem = service.findByLabel(it.label)
                    }
                }

                first = false
            } else {
                if (catalogueItem) {
                    //Try to find the child of this catalogue item by prefix and label
                    service = catalogueItemServices.find { node.typePrefix }

                    //Use the service to find a child CatalogueItem whose parent is catalogueItem and which has the specified label
                    //catalogueItem = service.findByParentAndLabel(catalogueItem, node.label)
                }
            }
        }

        catalogueItem
    }

}
