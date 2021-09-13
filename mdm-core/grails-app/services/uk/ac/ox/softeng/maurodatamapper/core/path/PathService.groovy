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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class PathService {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    @Autowired(required = false)
    List<MdmDomainService> domainServices

    @Autowired(required = false)
    List<SecurableResourceService> securableResourceServices

    GrailsApplication grailsApplication

    SecurableResource findSecurableResourceByDomainClassAndId(Class resourceClass, UUID resourceId) {
        SecurableResourceService securableResourceService = securableResourceServices.find {it.handles(resourceClass)}
        if (!securableResourceService) throw new ApiBadRequestException('PS03', "No service available to handle [${resourceClass.simpleName}]")
        securableResourceService.get(resourceId)
    }

    Map<String, String> listAllPrefixMappings() {
        List<MdmDomain> domains = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)
            .findAll {MdmDomain.isAssignableFrom(it.clazz) && !it.isAbstract()}
            .collect {grailsClass ->
                // Allow unqualified path domains to exist without breaking the system
                MdmDomain domain = grailsClass.newInstance() as MdmDomain
                domain.pathPrefix ? domain : null
            }.findAll()

        domains.collectEntries {domain ->
            [domain.pathPrefix, domain.domainType]
        }.sort() as Map<String, String>
    }

    MdmDomain findResourceByPathFromRootResource(MdmDomain rootResourceOfPath, Path path, String modelIdentifierOverride = null) {
        log.trace('Searching for path {} inside {}:{}', path.toString(modelIdentifierOverride), rootResourceOfPath.pathPrefix,
                  rootResourceOfPath.pathIdentifier)
        if (path.isEmpty()) {
            // assume we're in an empty/relative root which means we want the root resource
            return rootResourceOfPath
        }

        // If the current path is absolute to the root then get the relative path so we can search into the root
        Path pathToFind = path.isAbsoluteTo(rootResourceOfPath, modelIdentifierOverride) ? path.childPath : path

        // If no nodes in the pathToFind then return the model
        if (pathToFind.isEmpty()) return rootResourceOfPath

        // Find the first child in the path
        PathNode childNode = pathToFind.first()

        MdmDomainService domainService = domainServices.find {service ->
            service.handlesPathPrefix(childNode.prefix)
        }

        if (!domainService) {
            log.warn("Unknown path prefix [${childNode.prefix}] in path")
            return null
        }

        log.trace('Found service [{}] to handle prefix [{}]', domainService.class.simpleName, childNode.prefix)
        def child = domainService.findByParentIdAndPathIdentifier(rootResourceOfPath.id, childNode.getFullIdentifier(modelIdentifierOverride))

        if (!child) {
            log.warn("Child [{}] does not exist in root resource [{}]", childNode, Path.from(rootResourceOfPath))
            return null
        }

        // Recurse down the path for that child
        findResourceByPathFromRootResource(child, pathToFind, modelIdentifierOverride)
    }

    MdmDomain findResourceByPathFromRootClass(Class<? extends SecurableResource> rootClass, Path path) {
        findResourceByPathFromRootClass(rootClass, path, null)
    }

    MdmDomain findResourceByPathFromRootClass(Class<? extends SecurableResource> rootClass, Path path, UserSecurityPolicyManager userSecurityPolicyManager) {
        if (path.isEmpty()) {
            throw new ApiBadRequestException('PS05', 'Must have a path to search')
        }

        PathNode rootNode = path.first()

        SecurableResourceService securableResourceService = securableResourceServices.find {it.handles(rootClass)}
        if (!securableResourceService) {
            throw new ApiBadRequestException('PS03', "No service available to handle [${rootClass.simpleName}]")
        }
        if (!(securableResourceService instanceof MdmDomainService)) {
            throw new ApiBadRequestException('PS04', "[${rootClass.simpleName}] is not a pathable resource")
        }

        MdmDomain rootResource = securableResourceService.findByParentIdAndPathIdentifier(null, rootNode.getFullIdentifier())
        if (!rootResource) return null

        // Confirm root resource exists and its prefix matches the pathed prefix
        // We dont need to check the prefix in the findResourceByPathFromRootResource method as we "have" a resource at this point
        // And all subsequent calls in that method use the prefix to find the domain service
        if (rootResource.pathPrefix != rootNode.prefix) {
            log.warn("Root resource prefix [${rootNode.prefix}] does not match the root class to search")
            return null
        }

        // Check readabliity if possible
        // If no policymanager then assume readability has already been performed
        // Cannot read root then return null
        if (
        userSecurityPolicyManager && !userSecurityPolicyManager.userCanReadSecuredResourceId(rootResource.getClass() as Class<? extends SecurableResource>, rootResource.id)) {
            return null
        }

        findResourceByPathFromRootResource(rootResource, path)
    }

    /**
     * CAUTION: This method does not check whether the resource found by Path is readable by any user.
     * @param path
     * @return CreatorAware resource found by Path
     */
    MdmDomain findResourceByPath(Path path) {
        if (path.isEmpty()) {
            throw new ApiBadRequestException('PS05', 'Must have a path to search')
        }

        PathNode rootNode = path.first()

        MdmDomainService domainService = domainServices.find {service ->
            service.handlesPathPrefix(rootNode.prefix)
        }

        if (!domainService) {
            log.warn("Unknown path prefix [${rootNode.prefix}] in path")
            return null
        }

        MdmDomain rootResource = domainService.findByParentIdAndPathIdentifier(null, rootNode.getFullIdentifier())

        if (!rootResource) return null

        findResourceByPathFromRootResource(rootResource, path)
    }

    List<UUID> findAllResourceIdsInPath(Path path) {

        List<UUID> ids = []
        UUID parentId = null
        path.each {node ->
            MdmDomainService domainService = findDomainServiceForPrefix(node.prefix)
            MdmDomain domain = domainService.findByParentIdAndPathIdentifier(parentId, node.getFullIdentifier())
            if (!domain) {
                throw new ApiInternalException('PSXX', "No domain found for path node [${node}] in path [${path}]")
            }
            ids << domain.id
            parentId = domain.id
        }
        ids
    }

    List<Path> findAllSecuredPathsForIds(List<UUID> ids) {
        securableResourceServices.collectMany {it.getAll(ids) ?: []}.collect {SecurableResource sr -> sr.path}
    }

    MdmDomainService findDomainServiceForPrefix(String prefix) {
        for (MdmDomainService service : domainServices) {
            if (service.handlesPathPrefix(prefix)) return service
        }
        throw new ApiInternalException('PSXX', "No domain service found for prefix [${prefix}]")
    }
}
