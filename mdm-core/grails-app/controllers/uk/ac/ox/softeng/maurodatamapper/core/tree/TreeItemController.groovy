/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import grails.rest.RestfulController
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@SuppressWarnings('GroovyAssignabilityCheck')
class TreeItemController extends RestfulController<TreeItem> implements MdmController {

    private static final String INCLUDE_IMPORTED_PARAM = 'includeImported'
    private static final String INCLUDE_DELETED_PARAM = 'includeDeleted'
    private static final String INCLUDE_DOCUMENT_SUPERSEDED_PARAM = 'includeDocumentSuperseded'
    private static final String INCLUDE_MODEL_SUPERSEDED_PARAM = 'includeModelSuperseded'
    private static final String NO_CACHE_PARAM = 'noCache'
    private static final String CONTAINERS_ONLY_PARAM = 'containersOnly'
    private static final String FOLDERS_ONLY_PARAM = 'foldersOnly'
    private static final String MODEL_CREATEABLE_ONLY_PARAM = 'modelCreatableOnly'

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: [], update: [], patch: [], delete: [], show: 'GET', index: 'GET']

    TreeItemService treeItemService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    TreeItemController() {
        super(TreeItem, true)
    }

    def show() {
        log.debug('Call to tree for drill down into container or catalogue item')

        if (params.containsKey('containerId')) {
            // Drill down into a container and provide the tree from that perspective only
            Container container = treeItemService.findTreeCapableContainer(params.containerClass, params.containerId)
            if (!container) return notFound(Container, params.containerId)
            respond treeItemList: treeItemService.buildDirectChildrenContainerTree(params.containerClass,
                                                                                   container,
                                                                                   currentUserSecurityPolicyManager,
                                                                                   shouldIncludeDocumentSupersededItems(),
                                                                                   shouldIncludeModelSupersededItems(),
                                                                                   shouldIncludeDeletedItems())
            return
        }
        // If id provided then build the tree for that item, as we build the containers on the default we will assume this a catalogue item
        // Interceptor will have confirmed allowance to read
        CatalogueItem catalogueItem = treeItemService.findTreeCapableCatalogueItem(params.catalogueItemClass, params.catalogueItemId)
        if (!catalogueItem) return notFound(CatalogueItem, params.catalogueItemId)

        respond treeItemList: treeItemService.buildCatalogueItemTree(catalogueItem, false, currentUserSecurityPolicyManager, shouldIncludeImportedItems())
    }

    def index() {
        log.debug('Call to tree index for {}', params.containerDomainType)
        if (params.boolean(NO_CACHE_PARAM) && securityPolicyManagerService) {
            log.debug('Reloading user security policy manager as "No Cache" requested')
            currentUserSecurityPolicyManager = securityPolicyManagerService.reloadUserSecurityPolicyManager(getCurrentUser().emailAddress)
        }

        // Only return the containers
        if (params.boolean(CONTAINERS_ONLY_PARAM) || params.boolean(FOLDERS_ONLY_PARAM)) {
            if (params.boolean(MODEL_CREATEABLE_ONLY_PARAM)) {
                return respond(treeItemService.buildModelCreatableContainerOnlyTree(params.containerClass,
                                                                                    currentUserSecurityPolicyManager,
                                                                                    shouldIncludeDocumentSupersededItems(),
                                                                                    shouldIncludeModelSupersededItems(),
                                                                                    shouldIncludeDeletedItems()))
            }
            return respond(treeItemService.buildContainerOnlyTree(params.containerClass,
                                                                  currentUserSecurityPolicyManager,
                                                                  shouldIncludeDocumentSupersededItems(),
                                                                  shouldIncludeModelSupersededItems(),
                                                                  shouldIncludeDeletedItems()))
        }

        // Default behaviour is to return the root tree of containers
        respond treeItemService.buildRootContainerTree(params.containerClass, currentUserSecurityPolicyManager,
                                                       shouldIncludeDocumentSupersededItems(),
                                                       shouldIncludeModelSupersededItems(),
                                                       shouldIncludeDeletedItems())
    }

    def search() {
        log.debug('Call to tree search for {}', params.containerClass)
        respond treeItemService.buildContainerSearchTree(params.containerClass, currentUserSecurityPolicyManager,
                                                         params.searchTerm as String, params.domainType as String,
                                                         shouldIncludeDocumentSupersededItems(),
                                                         shouldIncludeModelSupersededItems(),
                                                         shouldIncludeDeletedItems())
    }

    def documentationSupersededModels() {
        log.debug('Admin call to get all documentation superseded models')
        respond treeItemService.buildContainerTreeWithAllDocumentationSupersededModelsByType(params.containerClass,
                                                                                             currentUserSecurityPolicyManager,
                                                                                             params.modelDomainType)
    }

    def modelSupersededModels() {
        log.debug('Admin call to get all model superseded models')
        respond treeItemService.buildContainerTreeWithAllModelSupersededModelsByType(params.containerClass,
                                                                                     currentUserSecurityPolicyManager,
                                                                                     params.modelDomainType)
    }

    def deletedModels() {
        log.debug('Admin call to get all deleted models')
        respond treeItemService.buildContainerTreeWithAllDeletedModelsByType(params.containerClass,
                                                                             currentUserSecurityPolicyManager,
                                                                             params.modelDomainType)
    }

    def fullModelTree() {
        log.debug('Call to tree for model')
        Model model = treeItemService.findTreeCapableCatalogueItem(params.modelClass, params.modelId)
        if (!model) return notFound(Model, params.modelId)

        respond(treeItemService.buildFullModelTree(model, currentUserSecurityPolicyManager))
    }

    def ancestors() {
        log.debug('Call to tree for containing ancestors of a catalogue item')
        CatalogueItem catalogueItem = treeItemService.findTreeCapableCatalogueItem(params.catalogueItemClass, params.catalogueItemId)
        if (!catalogueItem) return notFound(CatalogueItem, params.catalogueItemId)

        respond(containerTreeItem:
                    treeItemService.buildCatalogueItemTreeWithAncestors(params.containerClass, catalogueItem, currentUserSecurityPolicyManager))

    }

    private boolean shouldIncludeImportedItems() {
        params.boolean(INCLUDE_IMPORTED_PARAM)
    }

    private boolean shouldIncludeDeletedItems() {
        // Not admin, no see deleted items
        if (!currentUserSecurityPolicyManager.applicationAdministrator) return false
        params.boolean(INCLUDE_DELETED_PARAM)
    }

    private boolean shouldIncludeDocumentSupersededItems() {
        params.boolean(INCLUDE_DOCUMENT_SUPERSEDED_PARAM)
    }

    private boolean shouldIncludeModelSupersededItems() {
        params.boolean(INCLUDE_MODEL_SUPERSEDED_PARAM)
    }
}
