package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.session.SessionController* Controller: session
 * |   GET   | /api/admin/activeSessions       | Action: activeSessions       |
 * |   GET   | /api/sessions/isAuthenticated       | Action: isAuthenticatedSession       |
 */
@Integration
@Slf4j
class SessionFunctionalSpec extends BaseFunctionalSpec {

    String getResourcePath() {
        ''
    }

    void 'get activeSessions endpoint'() {
        when:
        GET('admin/activeSessions')

        and:
        String sessionId = currentCookie.getValue()

        then:
        verifyResponse OK, response

        and:
        response.body().count > 0

        when:
        Map info = response.body().items.find {it.id == sessionId}

        then:
        info.id == sessionId
        info.lastAccessedDateTime
        info.creationDateTime
        info.userEmailAddress
        info.lastAccessedUrl
    }

    void 'get is authenticated session endpoint'() {
        when:
        GET('session/isAuthenticated')

        then:
        verifyResponse OK, response

        and:
        response.body().authenticatedSession == false
    }
}
