package uk.ac.ox.softeng.maurodatamapper.core.email

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor


class EmailInterceptor implements MdmInterceptor {

    boolean before() {
        currentUserSecurityPolicyManager.isApplicationAdministrator() ?: unauthorised()
    }
}
