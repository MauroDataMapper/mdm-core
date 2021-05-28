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
package uk.ac.ox.softeng.maurodatamapper.core.tree

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ContainerTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class TreeItemService {

    @Autowired(required = false)
    List<ContainerService> containerServices

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    /**
     * Convenience method for obtaining the catalogue item from it's ID.
     * The item returned will extend Model or ModelItem.
     *
     * @param catalogueItemId
     * @return
     */
    CatalogueItem findTreeCapableCatalogueItem(Class catalogueItemClass, UUID catalogueItemId) {
        if (!catalogueItemId) return null

        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItemClass)}
        if (!service) throw new ApiBadRequestException('TIS01',
                                                       "Catalogue Item retrieval for catalogue item [${catalogueItemClass.simpleName}] with no " +
                                                       "supporting service")
        service.get(catalogueItemId)
    }

    Container findTreeCapableContainer(Class containerClass, UUID containerId) {
        if (!containerId) return null

        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS01',
                                                       "Container retrieval for container [${containerClass.simpleName}] with no " +
                                                       "supporting service")
        service.get(containerId)
    }

    /**
     * Obtain a complete tree containing all the containers the user is allowed to read
     *
     * @param containerClass
     * @param userSecurityPolicyManager
     * @param removeEmptyContainers
     * @return
     */
    def <K extends Container> List<TreeItem> buildContainerOnlyTree(Class<K> containerClass, UserSecurityPolicyManager userSecurityPolicyManager,
                                                                    boolean removeEmptyContainers) {
        log.info('Creating container only tree')
        buildContainerTreeFromContainerTreeItems(getAllReadableContainerTreeItems(containerClass, userSecurityPolicyManager), removeEmptyContainers)
    }

    def <K extends Container> List<TreeItem> buildFocusContainerTree(Class<K> containerClass, K container, UserSecurityPolicyManager userSecurityPolicyManager,
                                                                     boolean includeDocumentSuperseded, boolean includeModelSuperseded,
                                                                     boolean includeDeleted, boolean removeEmptyContainers) {
        log.info('Creating container drill tree')
        long start = System.currentTimeMillis()


        List<ContainerTreeItem> fullTree = buildContainerTree(containerClass, userSecurityPolicyManager, includeDocumentSuperseded, includeModelSuperseded,
                                                              includeDeleted, removeEmptyContainers)
        ContainerTreeItem treeItem = drillDownIntoTree(fullTree, container.id)
        log.debug('Container drill tree build took: {}', Utils.timeTaken(start))
        treeItem.children
    }

    ContainerTreeItem drillDownIntoTree(List<ContainerTreeItem> containerTreeItems, UUID containerId) {
        if (!containerTreeItems) return null
        for (ContainerTreeItem child : containerTreeItems) {
            ContainerTreeItem found = drillDownIntoTree(child, containerId)
            if (found) return found
        }
        null
    }

    ContainerTreeItem drillDownIntoTree(ContainerTreeItem treeItem, UUID containerId) {
        if (treeItem.id == containerId) return treeItem
        List<ContainerTreeItem> containerTreeItems = treeItem.children.findAll {it instanceof ContainerTreeItem} as List<ContainerTreeItem>
        drillDownIntoTree(containerTreeItems, containerId)
    }

    /**
     * Obtain a tree containing all the containers and models the user is allowed to read.
     * This will obtain all the models a user can read then wrap in containers, adding all additional containers which the user can read but may not
     * have any content. Use of the parameter "removeEmptyContainers" will remove all containers which have no models, a container which only has
     * containers inside will be removed.
     *
     * @param containerClass
     * @param userSecurityPolicyManager
     * @param includeDocumentSuperseded
     * @param includeModelSuperseded
     * @param includeDeleted
     * @param removeEmptyContainers
     * @return
     */
    def <K extends Container> List<ContainerTreeItem> buildContainerTree(Class<K> containerClass, UserSecurityPolicyManager userSecurityPolicyManager,
                                                                         boolean includeDocumentSuperseded, boolean includeModelSuperseded,
                                                                         boolean includeDeleted, boolean removeEmptyContainers) {
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS02', "Tree requested for container class ${containerClass} with no supporting service")

        // Virtual containers can be assigned to multiple objects, therefore the tree should only be the containers,
        // to view the objects in the container a separate call to the container should be made
        // Never remove empty containers
        if (service.isContainerVirtual()) {
            return buildContainerOnlyTree(containerClass, userSecurityPolicyManager, false)
        }

        log.info('Creating container tree')
        long start = System.currentTimeMillis()

        List<ModelTreeItem> readableModelTreeItems = getAllReadableModelTreeItems(userSecurityPolicyManager, service.containerPropertyNameInModel,
                                                                                  includeDocumentSuperseded,
                                                                                  includeModelSuperseded, includeDeleted)
        log.debug("Found ${readableModelTreeItems.size()} model tree items")
        List<ContainerTreeItem> tree = buildContainerTreeForModelTreeItems(containerClass, userSecurityPolicyManager, readableModelTreeItems,
                                                                           removeEmptyContainers)
        log.debug('Container tree build took: {}', Utils.timeTaken(start))
        tree
    }

    def <K extends Container> List<ContainerTreeItem> buildContainerSearchTree(Class<K> containerClass,
                                                                               UserSecurityPolicyManager userSecurityPolicyManager,
                                                                               String searchTerm, String domainType) {
        log.info('Creating searched container tree')
        if (!searchTerm) {
            log.debug('No search term')
            return []
        }
        long start = System.currentTimeMillis()
        List<ContainerTreeItem> tree = []
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for containers with no supporting service')

        if (!domainType || domainType != containerClass.simpleName) {
            log.debug('Searching catalogue item domain type [{}] and wrapping with container class {}', domainType ?: 'all',
                      containerClass.simpleName)
            List<ModelTreeItem> readableModelTreeItems = findAllReadableModelTreeItemsBySearchTermAndDomain(userSecurityPolicyManager,
                                                                                                            service.containerPropertyNameInModel,
                                                                                                            searchTerm, domainType)
            log.debug("Found ${readableModelTreeItems.size()} model tree items")

            log.debug("Wrapping models in container tree items", containerClass.simpleName)
            List<ContainerTreeItem> containerTreeItems = getAllReadableContainerTreeItems(containerClass, userSecurityPolicyManager)
            tree = wrapModelTreeItemsInContainerTreeItems(readableModelTreeItems, containerTreeItems,
                                                          true)
        }
        if (!domainType || domainType == containerClass.simpleName) {
            log.debug('Searching container class for search term')
            List<ContainerTreeItem> foundContainerTreeItems = findAllReadableContainerTreeItemsBySearchTerm(
                containerClass, userSecurityPolicyManager, searchTerm)
            // This will return a flat list which will contain all items needed to build a tree if its empty
            log.debug('Adding searched container tree items to tree')
            tree = addAdditionalContainerTreeItemsToExistingTree(tree, foundContainerTreeItems)
        }

        log.debug('Searched container tree build took: {}', Utils.timeTaken(start))
        tree
    }


    /**
     * Obtain the tree content for the provided catalogue item.
     * We assume security has been checked to ensure this item is readable by the user (done by interceptor).
     * A catalogue item will either be a Model or ModelItem, however the content of either will always be ModelItems, therefore this method
     * returns a list if ModelItemTreeItems.
     *
     * @param catalogueItem
     * @return
     */
    List<ModelItemTreeItem> buildCatalogueItemTree(CatalogueItem catalogueItem) {
        buildCatalogueItemTree(catalogueItem, false)
    }

    List<ModelItemTreeItem> buildCatalogueItemTree(CatalogueItem catalogueItem, boolean fullTreeRender) {
        log.info("Building tree for ${catalogueItem.class.simpleName}")
        long start = System.currentTimeMillis()

        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItem.class)}

        if (!service) throw new ApiBadRequestException('TIS01', 'Tree requested for catalogue item with no supporting service')

        if (!service.hasTreeTypeModelItems(catalogueItem, fullTreeRender)) {
            log.debug('Catalogue Item has no model items')
            return []
        }

        List<ModelItem> content = service.findAllTreeTypeModelItemsIn(catalogueItem, fullTreeRender)
        log.debug('Catalogue item has {} model items', content.size())
        List<ModelItemTreeItem> tree = content.collect {mi ->
            if (fullTreeRender) {
                List<ModelItemTreeItem> children = buildCatalogueItemTree(mi, true)
                ModelItemTreeItem modelItemTreeItem = new ModelItemTreeItem(mi, !children.isEmpty()).withRenderChildren()
                modelItemTreeItem.addAllToChildren(children) as ModelItemTreeItem
            } else new ModelItemTreeItem(mi, mi.hasChildren())
        }

        log.debug("Tree build took ${Utils.timeTaken(start)}")
        tree.sort()
    }

    ModelTreeItem buildFullModelTree(Model model) {
        log.info("Building full model tree for ${model.class.simpleName}")
        long start = System.currentTimeMillis()


        ModelService service = catalogueItemServices.find {it.handles(model.class)} as ModelService
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for model with no supporting service')

        if (!service.hasTreeTypeModelItems(model, true)) {
            return new ModelTreeItem(model, model.folder.id, false, false)
        }

        ModelTreeItem modelTreeItem = new ModelTreeItem(model, model.folder.id, true, false).withRenderChildren() as ModelTreeItem
        modelTreeItem.addAllToChildren buildCatalogueItemTree(model, true)

        log.debug("Tree build took ${Utils.timeTaken(start)}")
        modelTreeItem
    }

    def <K extends Container> List<ContainerTreeItem> buildContainerTreeWithAllDocumentationSupersededModelsByType(
        Class<K> containerClass, UserSecurityPolicyManager userSecurityPolicyManager, String modelDomainType) {

        ModelService service = modelServices.find {it.handles(modelDomainType)}
        if (!service) throw new ApiBadRequestException('AS01', "ModelService retrieval for model [${modelDomainType}] with no supporting service")
        List<Model> models = service.findAllDocumentationSupersededModels([:])
        List<UUID> modelIdsWithChildren = service.findAllModelIdsWithTreeChildren(models)
        buildContainerTreeForModelTreeItems(containerClass, userSecurityPolicyManager,
                                            getSupersededModelTreeItemsForModels(containerClass, models, modelIdsWithChildren),
                                            true)
    }

    def <K extends Container> List<ContainerTreeItem> buildContainerTreeWithAllModelSupersededModelsByType(
        Class<K> containerClass, UserSecurityPolicyManager userSecurityPolicyManager, String modelDomainType) {
        ModelService service = modelServices.find {it.handles(modelDomainType)}
        if (!service) throw new ApiBadRequestException('AS01', "ModelService retrieval for model [${modelDomainType}] with no supporting service")
        List<Model> models = service.findAllModelSupersededModels([:])
        List<UUID> modelIdsWithChildren = service.findAllModelIdsWithTreeChildren(models)
        buildContainerTreeForModelTreeItems(containerClass, userSecurityPolicyManager,
                                            getSupersededModelTreeItemsForModels(containerClass, models, modelIdsWithChildren),
                                            true)
    }

    def <K extends Container> List<ContainerTreeItem> buildContainerTreeWithAllDeletedModelsByType(
        Class<K> containerClass, UserSecurityPolicyManager userSecurityPolicyManager, String modelDomainType) {
        ModelService service = modelServices.find {it.handles(modelDomainType)}
        if (!service) throw new ApiBadRequestException('AS01', "ModelService retrieval for model [${modelDomainType}] with no supporting service")
        List<Model> models = service.findAllDeletedModels([:])
        List<UUID> supersededModels = service.findAllSupersededModelIds(models)
        List<UUID> modelIdsWithChildren = service.findAllModelIdsWithTreeChildren(models)
        List<ModelTreeItem> modelTreeItems = getModelTreeItemsForModels(containerClass, models, modelIdsWithChildren, supersededModels)
        buildContainerTreeForModelTreeItems(containerClass, userSecurityPolicyManager, modelTreeItems, true)
    }

    private <K extends Container> List<ModelTreeItem> getModelTreeItemsForModels(Class<K> containerClass, List<Model> models,
                                                                                 List<UUID> modelIdsWithChildren,
                                                                                 List<UUID> supersededIds) {
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for containers with no supporting service')
        models.collect {new ModelTreeItem(it, service.containerPropertyNameInModel, it.id in modelIdsWithChildren, supersededIds.contains(it.id))}
    }

    private <K extends Container> List<ModelTreeItem> getSupersededModelTreeItemsForModels(Class<K> containerClass, List<Model> models,
                                                                                           List<UUID> modelIdsWithChildren) {
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for containers with no supporting service')
        models.collect {new ModelTreeItem(it, service.containerPropertyNameInModel, it.id in modelIdsWithChildren, true)}
    }

    private <K extends Container> List<ContainerTreeItem> getAllReadableContainerTreeItems(Class<K> containerClass,
                                                                                           UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug("Getting all readable containers of type {}", containerClass.simpleName)
        List<UUID> readableContainerIds = userSecurityPolicyManager.listReadableSecuredResourceIds(containerClass)
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for containers with no supporting service')
        List<K> readableContainers = service.getAll(readableContainerIds)
        readableContainers.findAll().collect {new ContainerTreeItem(it as Container)}
    }

    private List<ModelTreeItem> getAllReadableModelTreeItems(UserSecurityPolicyManager userSecurityPolicyManager, String containerPropertyName,
                                                             boolean includeDocumentSuperseded, boolean includeModelSuperseded,
                                                             boolean includeDeleted) {
        List<ModelTreeItem> readableTreeItems = []

        modelServices.each {service ->

            log.debug('Getting all readable models of type {} ', service.modelClass.simpleName)
            long start1 = System.currentTimeMillis()
            List<? extends Model> readableModels = service.findAllReadableModels(userSecurityPolicyManager, includeDocumentSuperseded,
                                                                                 includeModelSuperseded, includeDeleted).sort()
            log.trace('Readable models took: {}', Utils.timeTaken(start1))

            long start2 = System.currentTimeMillis()
            List<UUID> modelsWithChildren = service.findAllModelIdsWithTreeChildren(readableModels)
            log.trace('Identifying models with children took: {}', Utils.timeTaken(start2))

            long start3 = System.currentTimeMillis()
            List<UUID> supersededModels = service.findAllSupersededModelIds(readableModels)
            log.trace('Identifying superseded models took: {}', Utils.timeTaken(start3))

            long start4 = System.currentTimeMillis()
            List<ModelTreeItem> treeItems = readableModels.collect {model ->
                new ModelTreeItem(model, containerPropertyName, modelsWithChildren.contains(model.ident()), supersededModels.contains(model.ident()))
            }
            log.trace('Listing tree items took: {}', Utils.timeTaken(start4))

            // Group by label to determine if branch name should be shown
            Map labelGrouping = treeItems.groupBy {it.label}
            labelGrouping.each {label, grouped ->
                int numberOfBranchNames = grouped.collect {it.branchName}.unique().size()
                log.trace('Label {} : Group count: {} : Branchname count: {}', label, grouped.size(), numberOfBranchNames)
                // If only model with that label then don't display branch name
                if (grouped.size() == 1) {
                    grouped.first().branchName = null
                } else if (numberOfBranchNames == 1) {
                    // If all the models have the same branch name then don't display it
                    grouped.each {it.branchName = null}
                }
            }
            log.trace('Branch name determination took: {}', Utils.timeTaken(start4))

            readableTreeItems.addAll(labelGrouping.collectMany {it.value})
            log.debug('Complete listing of {} took: {}', service.modelClass.simpleName, Utils.timeTaken(start1))

        }
        readableTreeItems
    }

    private <K extends Container> List<ContainerTreeItem> findAllReadableContainerTreeItemsBySearchTerm(
        Class<K> containerClass, UserSecurityPolicyManager userSecurityPolicyManager, String searchString) {

        log.debug('Searching models for search term [{}]', searchString)
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS03', 'Tree search requested for container with no supporting service')

        log.debug('Searching all readable containers of type {} ', containerClass.simpleName)
        long start1 = System.currentTimeMillis()
        List<K> containers = service.findAllReadableContainersBySearchTerm(userSecurityPolicyManager, searchString)
        log.debug('Found {} containers, took: {}', containers.size(), Utils.timeTaken(start1))

        List<ContainerTreeItem> foundContainerTreeItems = containers.collect {new ContainerTreeItem(it)}

        log.debug('Retrieving containers needed to wrap found containers')
        List<UUID> allContainerPathIds = foundContainerTreeItems.collect {it.pathIds}.flatten() as List<UUID>
        List<UUID> containerIdsNeeded = allContainerPathIds.findAll {!(it in foundContainerTreeItems*.id)}
        if (containerIdsNeeded) {
            List<K> extraContainers = service.getAll(containerIdsNeeded)
            foundContainerTreeItems.addAll(extraContainers.collect {new ContainerTreeItem(it)})
        }
        // Don't build a tree here as we need to add to an existing tree
        foundContainerTreeItems
    }

    private List<ModelTreeItem> findAllReadableModelTreeItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                   String containerPropertyName, String searchString,
                                                                                   String domainType) {
        log.debug('Searching catalogue items for label search term [{}] in domain [{}]', searchString, domainType ?: 'all')
        List<ModelTreeItem> foundTreeItems = []
        long start = System.currentTimeMillis()
        List<CatalogueItem> catalogueItems = []
        catalogueItemServices.each {service ->
            if (service.shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
                log.debug('Searching all readable catalogue items of type {} ', service.catalogueItemClass.simpleName)
                long start1 = System.currentTimeMillis()
                List<CatalogueItem> foundCatalogueItems =
                    service.findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(userSecurityPolicyManager,
                                                                                       searchString, domainType)
                log.debug('Found {} {} catalogue items, took: {}', foundCatalogueItems.size(), service.catalogueItemClass.simpleName,
                          Utils.timeTaken(start1))
                catalogueItems.addAll(foundCatalogueItems)
            }
        }
        log.debug('Found {} total catalogue items, took: {}', catalogueItems.size(), Utils.timeTaken(start))

        if (!catalogueItems) return []

        log.debug('Grouping catalogue items by model and model item')
        List<ModelItem> allModelItems = catalogueItems.findAll {it instanceof ModelItem}
        Map<String, List<Model>> domainTypeGroupedModels = catalogueItems.findAll {it instanceof Model}.groupBy {it.domainType}
        Map<String, List<ModelItem>> modelDomainTypeGroupedModelItems = allModelItems.groupBy {it.model.domainType}


        List<ModelTreeItem> foundModelTreeItems = []

        log.debug('Processing all found models and models for model items')
        modelServices.each {service ->

            String modelDomainType = service.getModelClass().simpleName
            List<Model> models = domainTypeGroupedModels.getOrDefault(modelDomainType, [])
            List<ModelItem> modelItems = modelDomainTypeGroupedModelItems.getOrDefault(modelDomainType, [])
            List<UUID> supersededModels = []

            if (modelItems) {
                log.debug('Retrieving models to wrap model items')
                List<UUID> modelIdsNeeded = modelItems.collect {it.model.id}.findAll {!(it in models*.id)}
                if (modelIdsNeeded) models.addAll(service.getAll(modelIdsNeeded))
            }

            if (models) {
                long start3 = System.currentTimeMillis()
                supersededModels = service.findAllSupersededModelIds(models)
                log.debug('Identifying superseded models took: {}', Utils.timeTaken(start3))
            }

            log.debug('Compiling model tree items for found items')
            foundModelTreeItems.addAll(
                models.collect {
                    new ModelTreeItem(it, containerPropertyName, null, supersededModels.contains(it.ident()))
                        .withRenderChildren() as ModelTreeItem
                }
            )
        }

        log.debug('Compiling model item tree items for found items')
        List<ModelItemTreeItem> foundModelItemTreeItems = allModelItems.collect {
            new ModelItemTreeItem(it, null).withRenderChildren() as ModelItemTreeItem
        }

        log.debug('Checking for non-root model tree items which are required due to found model items')
        foundModelItemTreeItems = checkForNonRootModelItemTreeItems(foundModelItemTreeItems)

        log.debug('Adding non-root tree items to root tree items')
        addNonRootTreeItemsToRootTreeItems(foundModelTreeItems, foundModelItemTreeItems)
        foundTreeItems.addAll(foundModelTreeItems)

        foundTreeItems
    }

    private List<ModelItemTreeItem> checkForNonRootModelItemTreeItems(List<ModelItemTreeItem> elementsToCheck) {
        if (!elementsToCheck) return []
        long start = System.currentTimeMillis()
        Set<UUID> nonRootIds = elementsToCheck.id.toSet()
        Set<UUID> requiredIds = ((List<UUID>) elementsToCheck.collect {it.allNonRootIds}.flatten()).toSet().findAll {!(it in nonRootIds)}
        elementsToCheck.addAll(getAllModelItemTreeItems(requiredIds))
        log.debug("Non root item check took ${System.currentTimeMillis() - start} ms. Size: ${elementsToCheck.size()}")
        return elementsToCheck
    }

    private List<ModelItemTreeItem> getAllModelItemTreeItems(Collection<UUID> parentIds) {
        log.debug('Searching for {} parents', parentIds.size())

        if (parentIds.isEmpty()) return []
        List<ModelItemTreeItem> modelItemTreeItems = []

        log.debug('Searching model item services for missing parent model items')
        for (ModelItemService service : modelItemServices) {

            List<ModelItem> modelItems = service.getAll(parentIds)
            if (modelItems) {
                modelItemTreeItems.addAll(
                    modelItems.collect {new ModelItemTreeItem(it, null).withRenderChildren() as ModelItemTreeItem}
                )
                parentIds.removeAll(modelItems.id)
            }
            // As we remove parentids as we go, there is a change all ids will be found and we dont want multiple objects that are the same
            // or to keep searching when we have nothing left to find
            if (!parentIds) break
        }

        log.debug('Found {} model item tree items', modelItemTreeItems.size())
        modelItemTreeItems
    }

    private <K extends Container> List<ContainerTreeItem> buildContainerTreeForModelTreeItems(Class<K> containerClass,
                                                                                              UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                              List<ModelTreeItem> modelTreeItems,
                                                                                              boolean removeEmptyContainers) {
        log.debug("Wrapping models in container tree items", containerClass.simpleName)
        List<ContainerTreeItem> containerTreeItems = getAllReadableContainerTreeItems(containerClass, userSecurityPolicyManager)
        wrapModelTreeItemsInContainerTreeItems(modelTreeItems, containerTreeItems, removeEmptyContainers)
    }

    private List<ContainerTreeItem> wrapModelTreeItemsInContainerTreeItems(List<ModelTreeItem> modelTreeItems,
                                                                           List<ContainerTreeItem> containerTreeItems,
                                                                           boolean removeEmptyContainers) {
        log.debug('Building the container tree')

        if (modelTreeItems) {
            log.debug('Adding model tree items to container tree items')
            modelTreeItems.groupBy {it.containerId}.each {UUID id, List<ModelTreeItem> results ->
                ContainerTreeItem parent = containerTreeItems.find {it.id == id}
                parent.addAllToChildren(results)
            }
        }
        buildContainerTreeFromContainerTreeItems(containerTreeItems, removeEmptyContainers)
    }

    private List<ContainerTreeItem> buildContainerTreeFromContainerTreeItems(List<ContainerTreeItem> containerTreeItems,
                                                                             boolean removeEmptyContainers) {
        log.debug('Building container tree from container tree items')
        log.trace('Identifying all root container tree items')
        long start = System.currentTimeMillis()
        final Set<ContainerTreeItem> rootTreeItems = containerTreeItems.findAll {!it.containerId}
        log.trace('Identifying all non-root container tree items')
        List<ContainerTreeItem> nonRootTreeItems = containerTreeItems.findAll {it.containerId}.sort {it.depth}

        addNonRootTreeItemsToRootTreeItems(rootTreeItems, nonRootTreeItems)

        if (removeEmptyContainers) removeEmptyContainerTreeItems(rootTreeItems)

        List<ContainerTreeItem> tree = rootTreeItems.sort()
        log.trace("Tree built took ${Utils.timeTaken(start)}")
        tree
    }

    private List<ContainerTreeItem> addAdditionalContainerTreeItemsToExistingTree(List<ContainerTreeItem> existingTree,
                                                                                  List<ContainerTreeItem> additionalContainerTreeItems) {
        log.debug('Adding additional container tree items to existing tree')
        log.trace('Identifying all root container tree items')
        long start = System.currentTimeMillis()
        Set<UUID> existingRootIds = existingTree.collect {it.id} as Set
        final Set<ContainerTreeItem> missingRootItems = additionalContainerTreeItems.findAll {!it.containerId && !(it.id in existingRootIds)}
        existingTree.addAll(missingRootItems)

        log.trace('Identifying all non-root container tree items')
        List<ContainerTreeItem> nonRootTreeItems = additionalContainerTreeItems.findAll {it.containerId}.sort {it.depth}

        addNonRootTreeItemsToRootTreeItems(existingTree, nonRootTreeItems)

        List<ContainerTreeItem> tree = existingTree.sort()
        log.trace("Tree built took ${Utils.timeTaken(start)}")
        tree
    }

    private void addNonRootTreeItemsToRootTreeItems(Collection<TreeItem> rootTreeItems, Collection<TreeItem> nonRootTreeItems) {
        log.debug('Adding non-root items to the root item list')
        Map<UUID, List<ContainerTreeItem>> grouped = nonRootTreeItems.groupBy {it.rootId}

        grouped.each {rootId, children ->
            log.trace('{} root, {} children', rootId, children.size())
            TreeItem rootItem = rootTreeItems.find {it.id == rootId}
            rootItem?.recursivelyAddToChildren(children)
        }
        rootTreeItems
    }

    private void removeEmptyContainerTreeItems(Set<ContainerTreeItem> rootTreeItems) {

        log.debug('Removing empty container tree items')
        rootTreeItems.removeIf({
            it.isEmptyContainerTree()
        })
        rootTreeItems.each {it.recursivelyRemoveEmptyChildContainers()}

    }

}
