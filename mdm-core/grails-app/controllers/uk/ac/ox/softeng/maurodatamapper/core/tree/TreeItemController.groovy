package uk.ac.ox.softeng.maurodatamapper.core.tree

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.rest.RestfulController
import groovy.util.logging.Slf4j

@Slf4j
class TreeItemController extends RestfulController<TreeItem> implements MdmController {

    private static final String INCLUDE_DELETED_PARAM = 'includeDeleted'
    private static final String INCLUDE_DOCUMENT_SUPERSEDED_PARAM = 'includeDocumentSuperseded'
    private static final String INCLUDE_MODEL_SUPERSEDED_PARAM = 'includeModelSuperseded'
    private static final String NO_CACHE_PARAM = 'noCache'
    private static final String CONTAINERS_ONLY_PARAM = 'containersOnly'
    private static final String FOLDERS_ONLY_PARAM = 'foldersOnly'

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: [], update: [], patch: [], delete: [], show: 'GET', index: 'GET']

    TreeItemService treeItemService

    TreeItemController() {
        super(TreeItem, true)
    }

    def show() {
        log.debug('Call to tree for catalogue item')
        // If id provided then build the tree for that item, as we build the containers on the default we will assume this a catalogue item
        // Interceptor will have confirmed allowance to read
        CatalogueItem catalogueItem = treeItemService.findTreeCapableCatalogueItem(Utils.toUuid(params.catalogueItemId))
        if (!catalogueItem) return notFound(params.catalogueItemId, CatalogueItem)

        respond(treeItemService.buildCatalogueItemTree(catalogueItem))
    }

    def index() {
        log.debug('Call to tree index for {}', params.containerDomainType)
        if (params.boolean(NO_CACHE_PARAM)) {
            // TODO
        }

        // Only return the containers
        if (params.boolean(CONTAINERS_ONLY_PARAM) || params.boolean(FOLDERS_ONLY_PARAM)) {
            return respond(treeItemService.buildContainerOnlyTree(params.containerClass, currentUserSecurityPolicyManager, false))
        }

        // Default behaviour is to return the tree of models and containing folders
        respond treeItemService.buildContainerTree(params.containerClass,
                                                   currentUserSecurityPolicyManager,
                                                   shouldIncludeDocumentSupersededItems(),
                                                   shouldIncludeModelSupersededItems(),
                                                   shouldIncludeDeletedItems(),
                                                   false)

    }

    def search() {
        log.debug('Call to tree search for {}', params.containerClass)
        respond treeItemService.buildContainerSearchTree(params.containerClass, currentUserSecurityPolicyManager,
                                                         params.searchTerm as String, params.domainType as String)
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
