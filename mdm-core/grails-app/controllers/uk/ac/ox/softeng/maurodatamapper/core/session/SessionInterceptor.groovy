package uk.ac.ox.softeng.maurodatamapper.core.session


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

class SessionInterceptor implements MdmInterceptor {

    SessionService sessionService

    SessionInterceptor() {
        match(uri: '/**/api/**/')
    }

    boolean before() {
        // Every session access will be added to the list by the session service as a sessionlistener
        sessionService.setLastAccessedUrl(session, request.requestURI)

        if (actionName == 'activeSessions') {
            return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: unauthorised()
        }

        true
    }
}