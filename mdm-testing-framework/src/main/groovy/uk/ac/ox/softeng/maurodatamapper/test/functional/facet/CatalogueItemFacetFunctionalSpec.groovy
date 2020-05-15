package uk.ac.ox.softeng.maurodatamapper.test.functional.facet

import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity

@Slf4j
abstract class CatalogueItemFacetFunctionalSpec<D extends GormEntity> extends ResourceFunctionalSpec<D> {

    abstract UUID getCatalogueItemId()

    abstract String getCatalogueItemDomainResourcePath()

    abstract String getFacetResourcePath()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainResourcePath()}/${getCatalogueItemId()}/${getFacetResourcePath()}"
    }
}