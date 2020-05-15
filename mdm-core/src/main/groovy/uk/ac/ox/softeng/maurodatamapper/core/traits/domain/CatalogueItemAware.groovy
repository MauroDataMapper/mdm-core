package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

/**
 * @since 30/01/2020
 */
trait CatalogueItemAware {

    UUID catalogueItemId
    String catalogueItemDomainType
    CatalogueItem catalogueItem

    abstract String getEditLabel()

    //static transients = ['catalogueItem']

    void setCatalogueItem(CatalogueItem catalogueItem) {
        this.catalogueItem = catalogueItem
        catalogueItemId = catalogueItem.id
        catalogueItemDomainType = catalogueItem.domainType
    }

}
