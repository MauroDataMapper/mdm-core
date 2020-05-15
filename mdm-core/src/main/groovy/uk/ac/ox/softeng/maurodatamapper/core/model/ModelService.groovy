package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

abstract class ModelService<K extends Model> extends CatalogueItemService<K> implements SecurableResourceService<K> {

    @Override
    Class<K> getCatalogueItemClass() {
        return getModelClass()
    }

    abstract Class<K> getModelClass()

    abstract List<K> findAllByContainerId(UUID containerId)

    abstract void deleteAllInContainer(Container container)

    abstract void removeAllFromContainer(Container container)

    abstract List<K> findAllReadableModels(UserSecurityPolicyManager userSecurityPolicyManager, boolean includeDocumentSuperseded,
                                           boolean includeModelSuperseded, boolean includeDeleted)

    abstract List<UUID> findAllModelIdsWithChildren(List<K> models)

    abstract void removeVersionLinkFromModel(UUID modelId, VersionLink versionLink)

    abstract List<UUID> findAllSupersededModelIds(List<K> models)

    abstract List<K> findAllDocumentationSupersededModels(Map pagination)

    abstract List<K> findAllModelSupersededModels(Map pagination)

    abstract List<K> findAllDeletedModels(Map pagination)

    @Override
    K checkFacetsAfterImportingCatalogueItem(K catalogueItem) {
        super.checkFacetsAfterImportingCatalogueItem(catalogueItem)
        if (catalogueItem.versionLinks) {
            catalogueItem.versionLinks.each {
                it.catalogueItem = catalogueItem
                it.createdBy = it.createdBy ?: catalogueItem.createdBy
            }
        }
        catalogueItem
    }

    @Override
    K updateFacetsAfterInsertingCatalogueItem(K catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.versionLinks) {
            catalogueItem.versionLinks.each {
                it.catalogueItemId = catalogueItem.getId()
            }
            VersionLink.saveAll(catalogueItem.versionLinks)
        }
        catalogueItem
    }
}