package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE

class SessionInterceptor implements MdmInterceptor {

    SessionService sessionService
    public static final Integer ORDER = LOWEST_PRECEDENCE + 2000

    SessionInterceptor() {
        match(uri: '/**/api/**/')
        order = ORDER
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