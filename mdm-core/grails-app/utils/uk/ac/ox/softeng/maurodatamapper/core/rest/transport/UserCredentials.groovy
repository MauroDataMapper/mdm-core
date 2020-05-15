package uk.ac.ox.softeng.maurodatamapper.core.rest.transport

import grails.validation.Validateable

/**
 * @since 02/10/2017
 */
class UserCredentials implements Validateable {
    String password
    String username

    static constraints = {
        username nullable: false, email: true
        password nullable: false
    }
}
