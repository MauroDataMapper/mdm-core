package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

class AnnotationController extends EditLoggingController<Annotation> {

    static responseFormats = ['json', 'xml']

    AnnotationService annotationService

    AnnotationController() {
        super(Annotation)
    }

    @Override
    protected Annotation queryForResource(Serializable resourceId) {
        annotationService.findByCatalogueItemIdAndId(params.catalogueItemId, resourceId)
    }

    @Override
    protected List<Annotation> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'dateCreated'
        params.order = params.order ?: 'desc'
        if (params.annotationId) {
            return annotationService.findAllByParentAnnotationId(params.annotationId, params)
        }

        return annotationService.findAllWhereRootAnnotationOfCatalogueItemId(params.catalogueItemId, params)
    }

    @Override
    void serviceDeleteResource(Annotation resource) {
        annotationService.delete(resource)
    }

    @Override
    protected Annotation createResource() {
        Annotation resource = super.createResource() as Annotation
        if (params.annotationId) {
            annotationService.get(params.annotationId)?.addToChildAnnotations(resource)
        }
        resource.catalogueItem = annotationService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)

        if (!resource.label && resource.parentAnnotation) {
            resource.label = "${resource.parentAnnotation.label} [${resource.parentAnnotation.childAnnotations.size()}]"
        }
        resource
    }

    @Override
    protected Annotation saveResource(Annotation resource) {
        resource.save flush: true, validate: false
        annotationService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected Annotation updateResource(Annotation resource) {
        resource.save flush: true, validate: false
        annotationService.addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected void deleteResource(Annotation resource) {
        serviceDeleteResource(resource)
        annotationService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }
}
