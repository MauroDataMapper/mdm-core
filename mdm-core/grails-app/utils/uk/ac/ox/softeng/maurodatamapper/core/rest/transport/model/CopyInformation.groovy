package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import grails.validation.Validateable

class CopyInformation implements Validateable {

    String copyLabel

    static constraints = {
        copyLabel blank: false
    }
}
