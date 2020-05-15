package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ClassifierController extends EditLoggingController<Classifier> {

    static responseFormats = ['json', 'xml']

    ClassifierService classifierService

    ClassifierController() {
        super(Classifier)
    }

    def catalogueItems() {
        classifierService.findAllReadableCatalogueItemsByClassifierId(currentUserSecurityPolicyManager, Utils.toUuid(params.classifierId), params)
    }

    @Override
    protected List<Classifier> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'label'
        if (params.catalogueItemId) {
            return classifierService.findAllByCatalogueItemId(currentUserSecurityPolicyManager, Utils.toUuid(params.catalogueItemId), params)
        }
        classifierService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(Classifier resource) {
        classifierService.delete(resource)
    }

    @Override
    protected Classifier saveResource(Classifier resource) {
        Classifier classifier = super.saveResource(resource) as Classifier

        if (params.catalogueItemId) {
            classifierService.addClassifierToCatalogueItem(params.catalogueItemDomainType, params.catalogueItemId, classifier)
        }
        classifier
    }

    @Override
    protected void deleteResource(Classifier resource) {
        if (params.catalogueItemId) {
            classifierService.removeClassifierFromCatalogueItem(params.catalogueItemDomainType, params.catalogueItemId, resource)
        } else {
            super.deleteResource(resource)
        }
    }
}
