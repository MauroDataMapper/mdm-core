package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController

import grails.rest.RestfulController

/**
 * @since 18/05/2022
 */
class DomainExportController extends RestfulController<DomainExport> implements MdmController {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: [], update: [], patch: [], delete: ['DELETE'], show: 'GET', index: 'GET']

    DomainExportService domainExportService

    DomainExportController() {
        super(DomainExport)
    }

    @Override
    def index(Integer max) {
        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, view: 'index'
    }

    @Override
    def show() {
        DomainExport resource = queryForResource(params.id)
        resource ? respond(resource, view: 'show') : notFound(params.id)
    }

    def download() {
        DomainExport resource = queryForResource(params.domainExportId)

        if (!resource) {
            return notFound(params.domainExportId)
        }

        render(file: resource.exportData, fileName: resource.exportFileName, contentType: resource.exportContentType)
    }

    @Override
    protected List<DomainExport> listAllResources(Map params) {
        currentUserSecurityPolicyManager.isApplicationAdministrator() ?
        domainExportService.list(params) :
        domainExportService.findAllReadableByUser(currentUserSecurityPolicyManager, params)

    }

    @Override
    protected DomainExport queryForResource(Serializable id) {
        domainExportService.get(id)
    }
}
