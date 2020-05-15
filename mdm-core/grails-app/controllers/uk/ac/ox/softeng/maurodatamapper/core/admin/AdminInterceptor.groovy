package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor


class AdminInterceptor implements MdmInterceptor {

    boolean before() {
        currentUserSecurityPolicyManager.isApplicationAdministrator() ?: unauthorised()
    }
}
