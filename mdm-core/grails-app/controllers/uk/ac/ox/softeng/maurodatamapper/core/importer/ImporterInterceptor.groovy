package uk.ac.ox.softeng.maurodatamapper.core.importer


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

class ImporterInterceptor implements MdmInterceptor {

    boolean before() {
        currentUserSecurityPolicyManager.isLoggedIn() ?: unauthorised()
    }
}
