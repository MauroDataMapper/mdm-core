package uk.ac.ox.softeng.maurodatamapper.core.email


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController

import grails.rest.RestfulController

class EmailController extends RestfulController<Email> implements MdmController {

    EmailService emailService

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: [], update: [], patch: [], delete: [], show: [], index: 'GET']

    EmailController() {
        super(Email)
    }

    @Override
    protected List<Email> listAllResources(Map params) {
        params.sort = params.sort ?: 'dateTimeSent'
        emailService.list(params)
    }
}
