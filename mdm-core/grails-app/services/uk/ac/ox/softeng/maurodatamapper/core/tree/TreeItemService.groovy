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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ContainerTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelItemTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ModelTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@GrailsCompileStatic
@Transactional
class TreeItemService {

    @Autowired(required = false)
    List<ContainerService> containerServices

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    PathService pathService

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
    def <K extends Container> List<ContainerTreeItem> buildContainerOnlyTree(Class<K> containerClass, UserSecurityPolicyManager userSecurityPolicyManager,
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
    List<ModelItemTreeItem> buildCatalogueItemTree(CatalogueItem catalogueItem, UserSecurityPolicyManager userSecurityPolicyManager) {
        buildCatalogueItemTree(catalogueItem, false, userSecurityPolicyManager)
    }

    List<ModelItemTreeItem> buildCatalogueItemTree(CatalogueItem catalogueItem, boolean fullTreeRender, UserSecurityPolicyManager userSecurityPolicyManager,
                                                   boolean includeImportedItems = false) {
        log.debug("Building tree for ${catalogueItem.class.simpleName}")
        long start = System.currentTimeMillis()

        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItem.class)}

        if (!service) throw new ApiBadRequestException('TIS01', 'Tree requested for catalogue item with no supporting service')

        if (!service.hasTreeTypeModelItems(catalogueItem, fullTreeRender, includeImportedItems)) {
            log.debug('Catalogue Item has no model items')
            return []
        }

        List<ModelItem> content = service.findAllTreeTypeModelItemsIn(catalogueItem, fullTreeRender, includeImportedItems)
        log.debug('Catalogue item has {} model items', content.size())
        List<ModelItemTreeItem> tree = content.collect {mi ->
            List<String> actions = userSecurityPolicyManager ?
                                   userSecurityPolicyManager.userAvailableTreeActions(mi.domainType, mi.id, mi.model.domainType, mi.model.id) :
                                   []
            if (fullTreeRender) {
                Collection<ModelItemTreeItem> children = buildCatalogueItemTree(mi, true, userSecurityPolicyManager)

                createModelItemTreeItem(mi, !children.isEmpty(), actions)
                    .withRenderChildren()
                    .addAllToChildren(children) as ModelItemTreeItem
            } else createModelItemTreeItem(mi, mi.hasChildren(), actions, service.isCatalogueItemImportedIntoCatalogueItem(mi, catalogueItem))
        }

        log.debug("Tree build took ${Utils.timeTaken(start)}")
        tree.sort()
    }

    def <K extends Container> ContainerTreeItem buildCatalogueItemTreeWithAncestors(Class<K> containerClass, CatalogueItem catalogueItem,
                                                                                    UserSecurityPolicyManager userSecurityPolicyManager) {
        TreeItem treeItem = recursiveAncestorModelItemTreeItemBuilder(catalogueItem, userSecurityPolicyManager)

        List<ModelTreeItem> ModelItemList = new ArrayList()
        ModelItemList.add(treeItem as ModelTreeItem)

        List<ContainerTreeItem> cti = buildContainerTreeForModelTreeItems(containerClass, userSecurityPolicyManager, ModelItemList, true)
        cti.first()
    }


    /**
     * Given a catalogue Item the method works its way up the tree to a non ModelItem (which should be a DataModel)
     * and builds a tree from the items along the way
     * It returns a generic TreeItem for recursion purposes but it is always a ModelTreeItem at the top end when it breaks out
     * @param catalogueItem
     * @return TreeItem
     */
    TreeItem recursiveAncestorModelItemTreeItemBuilder(CatalogueItem catalogueItem,
                                                       UserSecurityPolicyManager userSecurityPolicyManager) {

        if (catalogueItem instanceof ModelItem) {
            ModelItem mi = catalogueItem as ModelItem
            List<String> actions = userSecurityPolicyManager ?
                                   userSecurityPolicyManager.userAvailableTreeActions(mi.domainType, mi.id, mi.model.domainType, mi.model.id) :
                                   []
            List<ModelItemTreeItem> current = new ArrayList<>()
            current.add(createModelItemTreeItem(mi, mi.hasChildren(), actions))
            if (mi.getParent()) {
                TreeItem parent = recursiveAncestorModelItemTreeItemBuilder(mi.getParent(), userSecurityPolicyManager)
                parent.addAllToChildren(current)
                parent.renderChildren = true
                return parent
            }
        }
        Model model = catalogueItem as Model
        createModelTreeItem(model, model.folder.id, true, false, userSecurityPolicyManager)
    }

    ModelTreeItem buildFullModelTree(Model model, UserSecurityPolicyManager userSecurityPolicyManager) {
        log.info("Building full model tree for ${model.class.simpleName}")
        long start = System.currentTimeMillis()

        ModelService service = catalogueItemServices.find {it.handles(model.class)} as ModelService
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for model with no supporting service')

        if (!service.hasTreeTypeModelItems(model, true)) {
            createModelTreeItem(model, model.folder.id, false, false, userSecurityPolicyManager)
        }

        ModelTreeItem modelTreeItem = createModelTreeItem(model, model.folder.id, true, false, userSecurityPolicyManager)
            .withRenderChildren()
            .addAllToChildren(buildCatalogueItemTree(model, true, null)) as ModelTreeItem

        log.debug("Tree build took ${Utils.timeTaken(start)}")
        modelTreeItem
    }

    def <K extends Container> List<ContainerTreeItem> buildContainerTreeWithAllDocumentationSupersededModelsByType(Class<K> containerClass,
                                                                                                                   UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                                                   String modelDomainType) {

        ModelService service = modelServices.find {it.handles(modelDomainType)}
        if (!service) throw new ApiBadRequestException('AS01', "ModelService retrieval for model [${modelDomainType}] with no supporting service")
        List<Model> models = service.findAllDocumentationSupersededModels([:])
        List<UUID> modelIdsWithChildren = service.findAllModelIdsWithTreeChildren(models)
        buildContainerTreeForModelTreeItems(containerClass, userSecurityPolicyManager,
                                            getSupersededModelTreeItemsForModels(containerClass, models, modelIdsWithChildren, userSecurityPolicyManager),
                                            true)
    }

    def <K extends Container> List<ContainerTreeItem> buildContainerTreeWithAllModelSupersededModelsByType(Class<K> containerClass,
                                                                                                           UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                                           String modelDomainType) {
        ModelService service = modelServices.find {it.handles(modelDomainType)}
        if (!service) throw new ApiBadRequestException('AS01', "ModelService retrieval for model [${modelDomainType}] with no supporting service")
        List<Model> models = service.findAllModelSupersededModels([:])
        List<UUID> modelIdsWithChildren = service.findAllModelIdsWithTreeChildren(models)
        buildContainerTreeForModelTreeItems(containerClass, userSecurityPolicyManager,
                                            getSupersededModelTreeItemsForModels(containerClass, models, modelIdsWithChildren, userSecurityPolicyManager),
                                            true)
    }

    def <K extends Container> List<ContainerTreeItem> buildContainerTreeWithAllDeletedModelsByType(Class<K> containerClass,
                                                                                                   UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                                   String modelDomainType) {
        ModelService service = modelServices.find {it.handles(modelDomainType)}
        if (!service) throw new ApiBadRequestException('AS01', "ModelService retrieval for model [${modelDomainType}] with no supporting service")
        List<Model> models = service.findAllDeletedModels([:])
        List<UUID> supersededModels = service.findAllSupersededModelIds(models)
        List<UUID> modelIdsWithChildren = service.findAllModelIdsWithTreeChildren(models)
        List<ModelTreeItem> modelTreeItems = getModelTreeItemsForModels(containerClass, models, modelIdsWithChildren, supersededModels, userSecurityPolicyManager)
        buildContainerTreeForModelTreeItems(containerClass, userSecurityPolicyManager, modelTreeItems, true)
    }

    private <K extends Container> List<ModelTreeItem> getModelTreeItemsForModels(Class<K> containerClass, List<Model> models,
                                                                                 List<UUID> modelIdsWithChildren,
                                                                                 List<UUID> supersededIds,
                                                                                 UserSecurityPolicyManager userSecurityPolicyManager) {
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for containers with no supporting service')
        models.collect {model ->
            createModelTreeItem(model, service.containerPropertyNameInModel, model.id in modelIdsWithChildren, supersededIds.contains(model.id), userSecurityPolicyManager)
        }
    }

    private <K extends Container> List<ModelTreeItem> getSupersededModelTreeItemsForModels(Class<K> containerClass,
                                                                                           List<Model> models,
                                                                                           List<UUID> modelIdsWithChildren,
                                                                                           UserSecurityPolicyManager userSecurityPolicyManager) {
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for containers with no supporting service')
        models.collect {model ->
            createModelTreeItem(model, service.containerPropertyNameInModel, model.id in modelIdsWithChildren, true, userSecurityPolicyManager)
        }
    }

    private <K extends Container> List<ContainerTreeItem> getAllReadableContainerTreeItems(Class<K> containerClass,
                                                                                           UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug("Getting all readable containers of type {}", containerClass.simpleName)
        List<UUID> readableContainerIds = userSecurityPolicyManager.listReadableSecuredResourceIds(containerClass)
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS02', 'Tree requested for containers with no supporting service')
        List<K> readableContainers = service.getAll(readableContainerIds).findAll()
        readableContainers.collect {container ->
            createContainerTreeItem(container, userSecurityPolicyManager)
        }
    }

    private List<ModelTreeItem> getAllReadableModelTreeItems(UserSecurityPolicyManager userSecurityPolicyManager, String containerPropertyName,
                                                             boolean includeDocumentSuperseded, boolean includeModelSuperseded,
                                                             boolean includeDeleted) {
        if (!modelServices) return []
        List<ModelTreeItem> readableTreeItems = []

        modelServices.each {service ->

            log.debug('Getting all readable models of type {} ', service.domainClass.simpleName)
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
                createModelTreeItem(model, containerPropertyName, modelsWithChildren.contains(model.ident()), supersededModels.contains(model.ident()),
                                    userSecurityPolicyManager)
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
            List<ModelTreeItem> collectedReadableTreeItems = labelGrouping.collectMany {k, v -> v as Collection} as List<ModelTreeItem>
            readableTreeItems.addAll(collectedReadableTreeItems)
            log.debug('Complete listing of {} took: {}', service.domainClass.simpleName, Utils.timeTaken(start1))
        }
        readableTreeItems
    }

    private <K extends Container> List<ContainerTreeItem> findAllReadableContainerTreeItemsBySearchTerm(Class<K> containerClass,
                                                                                                        UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                                        String searchString) {

        log.debug('Searching models for search term [{}]', searchString)
        ContainerService service = containerServices.find {it.handles(containerClass)}
        if (!service) throw new ApiBadRequestException('TIS03', 'Tree search requested for container with no supporting service')

        log.debug('Searching all readable containers of type {} ', containerClass.simpleName)
        long start1 = System.currentTimeMillis()
        List<K> containers = service.findAllReadableContainersBySearchTerm(userSecurityPolicyManager, searchString) as List<K>
        log.debug('Found {} containers, took: {}', containers.size(), Utils.timeTaken(start1))

        List<ContainerTreeItem> foundContainerTreeItems = containers.collect {container ->
            createContainerTreeItem(container, userSecurityPolicyManager)
        }

        log.debug('Retrieving containers needed to wrap found containers')
        List<UUID> allContainerPathIds = foundContainerTreeItems.collect {it.pathIds}.flatten() as List<UUID>
        List<UUID> containerIdsNeeded = allContainerPathIds.findAll {!(it in foundContainerTreeItems*.id)}
        if (containerIdsNeeded) {
            List<K> extraContainers = service.getAll(containerIdsNeeded) as List<K>
            foundContainerTreeItems.addAll(extraContainers.collect {container ->
                createContainerTreeItem(container, userSecurityPolicyManager)
            })
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
                log.debug('Searching all readable catalogue items of type {} ', service.domainClass.simpleName)
                long start1 = System.currentTimeMillis()
                List<CatalogueItem> foundCatalogueItems =
                    service.findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(userSecurityPolicyManager,
                                                                                       searchString, domainType)
                log.debug('Found {} {} catalogue items, took: {}', foundCatalogueItems.size(), service.domainClass.simpleName,
                          Utils.timeTaken(start1))
                catalogueItems.addAll(foundCatalogueItems)
            }
        }
        log.debug('Found {} total catalogue items, took: {}', catalogueItems.size(), Utils.timeTaken(start))

        if (!catalogueItems) return []

        log.debug('Grouping catalogue items by model and model item')
        List<ModelItem> allModelItems = catalogueItems.findAll {it instanceof ModelItem} as List<ModelItem>

        Map<String, List<Model>> domainTypeGroupedModels = (catalogueItems
            .findAll {it instanceof Model}
            .groupBy {it.domainType} as Map<String, List<Model>>)

        Map<String, List<ModelItem>> modelDomainTypeGroupedModelItems = allModelItems.groupBy {it.model.domainType}

        List<ModelTreeItem> foundModelTreeItems = []

        log.debug('Processing all found models and models for model items')
        modelServices.each {service ->

            String modelDomainType = service.domainClass.simpleName
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
                models.collect {model ->
                    createModelTreeItem(model, containerPropertyName, null, supersededModels.contains(model.ident()), userSecurityPolicyManager)
                        .withRenderChildren() as ModelTreeItem
                }
            )
        }

        log.debug('Compiling model item tree items for found items')
        List<ModelItemTreeItem> foundModelItemTreeItems = allModelItems.collect {modelItem ->
            createModelItemTreeItem(modelItem, null,
                                    userSecurityPolicyManager.userAvailableTreeActions(modelItem.domainType, modelItem.id, modelItem.model.domainType, modelItem.model.id)
            ).withRenderChildren() as ModelItemTreeItem
        }

        log.debug('Checking for non-root model tree items which are required due to found model items')
        foundModelItemTreeItems = checkForNonRootModelItemTreeItems(foundModelItemTreeItems, userSecurityPolicyManager)

        log.debug('Adding non-root tree items to root tree items')
        addNonRootTreeItemsToRootTreeItems(foundModelTreeItems, foundModelItemTreeItems)
        foundTreeItems.addAll(foundModelTreeItems)

        foundTreeItems
    }

    private List<ModelItemTreeItem> checkForNonRootModelItemTreeItems(List<ModelItemTreeItem> elementsToCheck,
                                                                      UserSecurityPolicyManager userSecurityPolicyManager) {
        if (!elementsToCheck) return []
        long start = System.currentTimeMillis()
        Set<UUID> nonRootIds = elementsToCheck.id.toSet()
        Set<UUID> requiredIds = ((List<UUID>) elementsToCheck.collect {it.allNonRootIds}.flatten()).toSet().findAll {!(it in nonRootIds)}
        elementsToCheck.addAll(getAllModelItemTreeItems(requiredIds, userSecurityPolicyManager))
        log.debug("Non root item check took ${System.currentTimeMillis() - start} ms. Size: ${elementsToCheck.size()}")
        return elementsToCheck
    }

    private List<ModelItemTreeItem> getAllModelItemTreeItems(Collection<UUID> parentIds, UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug('Searching for {} parents', parentIds.size())

        if (parentIds.isEmpty()) return []
        List<ModelItemTreeItem> modelItemTreeItems = []

        log.debug('Searching model item services for missing parent model items')
        for (ModelItemService service : modelItemServices) {

            List<ModelItem> modelItems = service.getAll(parentIds) as List<ModelItem>
            if (modelItems) {
                modelItemTreeItems.addAll(
                    modelItems.collect {modelItem ->
                        createModelItemTreeItem(modelItem, null,
                                                userSecurityPolicyManager.
                                                    userAvailableTreeActions(modelItem.domainType, modelItem.id, modelItem.model.domainType, modelItem.model.id))
                            .withRenderChildren() as ModelItemTreeItem
                    }
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
        final Set<ContainerTreeItem> rootTreeItems = containerTreeItems.findAll {!it.containerId}.toSet()
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
        final Set<ContainerTreeItem> missingRootItems = additionalContainerTreeItems.findAll {!it.containerId && !(it.id in existingRootIds)}.toSet()
        existingTree.addAll(missingRootItems)

        log.trace('Identifying all non-root container tree items')
        List<ContainerTreeItem> nonRootTreeItems = additionalContainerTreeItems.findAll {it.containerId}.sort {it.depth}

        addNonRootTreeItemsToRootTreeItems(existingTree, nonRootTreeItems)

        List<ContainerTreeItem> tree = existingTree.sort()
        log.trace("Tree built took ${Utils.timeTaken(start)}")
        tree
    }

    private void addNonRootTreeItemsToRootTreeItems(Collection<? extends TreeItem> rootTreeItems, Collection<? extends TreeItem> nonRootTreeItems) {
        log.debug('Adding non-root items to the root item list')
        Map<UUID, List<ContainerTreeItem>> grouped = (nonRootTreeItems.groupBy {it.rootId} as Map<UUID, List<ContainerTreeItem>>)

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

    ModelItemTreeItem createModelItemTreeItem(ModelItem modelItem, Boolean childrenExist, List<String> availableTreeActions, boolean imported = false) {
        contextualiseTreeItem(new ModelItemTreeItem(modelItem, childrenExist, availableTreeActions, imported))
    }

    @CompileDynamic
    private ModelTreeItem createModelTreeItem(Model model, String containerPropertyName, Boolean childrenExist, Boolean isSuperseded,
                                              UserSecurityPolicyManager userSecurityPolicyManager) {
        createModelTreeItem(model, model."$containerPropertyName".id as UUID, childrenExist, isSuperseded, userSecurityPolicyManager)
    }

    private ModelTreeItem createModelTreeItem(Model model, UUID containerId, Boolean childrenExist, Boolean isSuperseded,
                                              UserSecurityPolicyManager userSecurityPolicyManager) {
        contextualiseTreeItem(new ModelTreeItem(model, containerId, childrenExist, isSuperseded,
                                                userSecurityPolicyManager.userAvailableTreeActions(model.domainType, model.id)))
    }

    private ContainerTreeItem createContainerTreeItem(Container container, UserSecurityPolicyManager userSecurityPolicyManager) {
        contextualiseTreeItem(new ContainerTreeItem(container,
                                                    userSecurityPolicyManager.userAvailableTreeActions(container.domainType, container.id)))
    }

    private <T extends TreeItem> T contextualiseTreeItem(T treeItem) {

        List<UUID> pathIds = pathService.findAllResourceIdsInPath(treeItem.path)

        // Remove the last id in the path as its the id of the TI
        pathIds.removeLast()

        treeItem.pathIds = pathIds
        treeItem.rootId = pathIds ? pathIds.first() : null
        treeItem.parentId = pathIds ? pathIds.last() : null

        treeItem
    }
}
