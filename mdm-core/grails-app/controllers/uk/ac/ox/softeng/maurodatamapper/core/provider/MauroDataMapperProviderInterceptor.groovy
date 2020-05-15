package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor


class MauroDataMapperProviderInterceptor implements MdmInterceptor {

    boolean before() {
        currentUserSecurityPolicyManager.isApplicationAdministrator() ?: unauthorised()
    }
}
