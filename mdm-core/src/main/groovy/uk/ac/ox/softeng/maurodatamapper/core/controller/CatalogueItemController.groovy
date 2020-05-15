package uk.ac.ox.softeng.maurodatamapper.core.controller

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.util.logging.Slf4j

/**
 * @since 02/04/2020
 */
@Slf4j
abstract class CatalogueItemController<T extends CatalogueItem> extends EditLoggingController<T> {

    CatalogueItemController(Class resource) {
        super(resource)
    }

    CatalogueItemController(Class resource, boolean readOnly) {
        super(resource, readOnly)
    }

    abstract protected void serviceInsertResource(T resource)

    @Override
    protected T saveResource(T resource) {
        log.trace('save resource')
        serviceInsertResource(resource)
        if (!params.boolean('noHistory')) resource.addCreatedEdit(currentUser)
        resource
    }
}
