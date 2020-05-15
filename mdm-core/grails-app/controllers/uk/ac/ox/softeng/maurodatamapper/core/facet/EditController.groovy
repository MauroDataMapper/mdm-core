package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController

import grails.rest.RestfulController

class EditController extends RestfulController<Edit> implements MdmController {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: [], update: [], patch: [], delete: [], show: [], index: 'GET']

    EditService editService

    EditController() {
        super(Edit)
    }

    EditController(boolean readOnly) {
        super(Edit, readOnly)
    }

    @Override
    protected List<Edit> listAllResources(Map params) {
        params.sort = params.sort ?: 'dateCreated'
        params.order = params.order ?: 'asc'

        editService.findAllByResource(params.resourceDomainType, params.resourceId, params)
    }
}
