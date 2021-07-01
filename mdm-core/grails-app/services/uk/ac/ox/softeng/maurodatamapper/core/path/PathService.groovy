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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.DomainService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Path
import uk.ac.ox.softeng.maurodatamapper.util.PathNode
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.gorm.transactions.Transactional
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class PathService {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    @Autowired(required = false)
    List<DomainService> domainServices

    @Autowired(required = false)
    List<SecurableResourceService> securableResourceServices

    GrailsApplication grailsApplication

    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler()

    SecurableResource findSecurableResourceByDomainClassAndId(Class resourceClass, UUID resourceId) {
        SecurableResourceService securableResourceService = securableResourceServices.find { it.handles(resourceClass) }
        if (!securableResourceService) throw new ApiBadRequestException('PS03', "No service available to handle [${resourceClass.simpleName}]")
        securableResourceService.get(resourceId)
    }

    Map<String, String> listAllPrefixMappings() {
        grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)
            .findAll { CreatorAware.isAssignableFrom(it.clazz) && !it.isAbstract() }
            .collectEntries { grailsClass ->
                def domain = grailsClass.newInstance()
                // Allow unqualified path domains to exist without breaking the system
                if (domain instanceof CreatorAware && domain.pathPrefix) {
                    [domain.pathPrefix, domain.domainType]
                }
                null
            }.findAll().sort() as Map<String, String>
    }

    CreatorAware findResourceByPathFromRootResource(CreatorAware rootResourceOfPath, Path path) {
        if (path.isEmpty()) {
            throw new ApiBadRequestException('PS06', 'Must have a path to search')
        }

        if (path.first().label != rootResourceOfPath.pathIdentifier) {
            throw new ApiBadRequestException('PS01', 'Path cannot exist inside resource as first path node is not the resource node')
        }
        // Confirmed the path is inside the model
        // If only one node then return the model
        if (path.size == 1) return rootResourceOfPath as CreatorAware

        // Only 2 nodes in path, first is model
        // Last part of path is a field access as has no type prefix so return the model
        if (path.size == 2 && !path.last().hasTypePrefix()) return rootResourceOfPath as CreatorAware

        // Find the first child in the path
        Path childPath = path.childPath
        PathNode childNode = childPath.first()

        DomainService domainService = domainServices.find { service ->
            service.handlesPathPrefix(childNode.typePrefix)
        }

        log.debug('Found service [{}] to handle [{}]', domainService.class.simpleName, childNode.typePrefix)
        CreatorAware child = domainService.findByParentIdAndPathIdentifier(rootResourceOfPath.id, childNode.label)

        if (!child) {
            log.debug("Child [${childNode}] does not exist in path [${path}]")
            throw new ApiBadRequestException('PS02', "Child [${childNode}] in path cannot be found inside domain")
        }

        // Recurse down the path for that child
        findResourceByPathFromRootResource(child, childPath)
    }

    CreatorAware findResourceByPathFromRootClass(Class<? extends SecurableResource> rootClass, Path path) {
        if (path.isEmpty()) {
            throw new ApiBadRequestException('PS05', 'Must have a path to search')
        }

        PathNode rootNode = path.first()

        SecurableResourceService securableResourceService = securableResourceServices.find { it.handles(rootClass) }
        if (!securableResourceService) {
            throw new ApiBadRequestException('PS03', "No service available to handle [${rootClass.simpleName}]")
        }
        if (!(securableResourceService instanceof DomainService)) {
            throw new ApiBadRequestException('PS04', "[${rootClass.simpleName}] is not a pathable resource")
        }

        CreatorAware rootResource = securableResourceService.findByParentIdAndPathIdentifier(null, rootNode.label)
        if (!rootResource) return null
        findResourceByPathFromRootResource(rootResource, path)
    }

    @Deprecated
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
                        if (service instanceof ModelService) {
                            catalogueItem = service.findLatestModelByLabel(node.label)
                        } else {
                            catalogueItem = service.findByLabel(node.label)
                        }
                    }
                }

                /*
                Only return anything if the first item retrieved is a model which is securable and readable, or it belongs to a model which is
                securable and readable
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

    GrailsClass getGrailsResource(GrailsParameterMap params, String resourceParam) {
        String lookup = params[resourceParam]

        if (!lookup) {
            throw new ApiBadRequestException('MCI01', "No domain class resource provided")
        }

        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, lookup)
        if (!grailsClass) {
            throw new ApiBadRequestException('MCI02', "Unrecognised domain class resource [${params[resourceParam]}]")
        }
        grailsClass
    }

}
