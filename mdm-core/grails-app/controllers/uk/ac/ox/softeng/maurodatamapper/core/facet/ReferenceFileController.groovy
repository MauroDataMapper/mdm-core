package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

import grails.web.mime.MimeType
import org.grails.web.servlet.mvc.GrailsWebRequest

class ReferenceFileController extends EditLoggingController<ReferenceFile> {
    static responseFormats = ['json', 'xml']

    ReferenceFileService referenceFileService

    ReferenceFileController() {
        super(ReferenceFile)
    }

    @Override
    def show() {
        def resource = queryForResource(params.id)
        if (resource) {
            return render(file: resource.fileContents, fileName: resource.fileName, contentType: resource.contentType)
        }
        return notFound()
    }

    @Override
    protected ReferenceFile queryForResource(Serializable resourceId) {
        return referenceFileService.findByCatalogueItemIdAndId(params.catalogueItemId, resourceId)
    }

    @Override
    protected List<ReferenceFile> listAllReadableResources(Map params) {
        return referenceFileService.findAllByCatalogueItemId(params.catalogueItemId, params)
    }

    @Override
    protected ReferenceFile createResource() {
        ReferenceFile resource = super.createResource() as ReferenceFile
        resource.determineFileType()
        resource.catalogueItem = referenceFileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    void serviceDeleteResource(ReferenceFile resource) {
        referenceFileService.delete(resource)
    }

    @Override
    protected ReferenceFile saveResource(ReferenceFile resource) {
        resource.save flush: true, validate: false
        referenceFileService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected ReferenceFile updateResource(ReferenceFile resource) {
        resource.save flush: true, validate: false
        referenceFileService.addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected void deleteResource(ReferenceFile resource) {
        serviceDeleteResource(resource)
        referenceFileService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }


    @Override
    protected Object getObjectToBind() {
        if (request.contentType.startsWith(MimeType.MULTIPART_FORM.name)) {
            GrailsWebRequest grailsWebRequest = GrailsWebRequest.lookup(request)
            Map object = grailsWebRequest.params
            return object
        }
        request
    }
}
