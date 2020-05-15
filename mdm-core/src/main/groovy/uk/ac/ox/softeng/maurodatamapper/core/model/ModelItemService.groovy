package uk.ac.ox.softeng.maurodatamapper.core.model

abstract class ModelItemService<K extends ModelItem> extends CatalogueItemService<K> {

    @Override
    Class<K> getCatalogueItemClass() {
        return getModelItemClass()
    }

    abstract Class<K> getModelItemClass()

    abstract K updateIndexForModelItemInParent(K modelItem, CatalogueItem parent, int newIndex)
}