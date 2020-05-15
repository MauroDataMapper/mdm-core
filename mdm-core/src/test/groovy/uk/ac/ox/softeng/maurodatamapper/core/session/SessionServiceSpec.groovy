package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest

import javax.servlet.http.HttpSession

/**
 * @since 03/05/2018
 */
class SessionServiceSpec extends BaseUnitSpec implements ServiceUnitTest<SessionService>, GrailsWebUnitTest {

    void 'test servlet context setup'() {
        expect:
        service.getActiveSessionMap(servletContext) == null

        when:
        initialiseContext()

        then:
        service.getActiveSessionMap(servletContext) != null
    }

    void 'create session info'() {
        given:
        initialiseContext()

        when:
        service.setUserEmailAddress(session, 'test@test.com')
        HttpSession httpSession = service.retrieveSession(servletContext, session.id)

        then:
        httpSession

        and:
        httpSession.getAttribute('emailAddress') == 'test@test.com'
        !httpSession.getAttribute('lastUrl')
    }

    void 'update session info'() {
        given:
        initialiseContext()
        service.setUserEmailAddress(session, 'test@test.com')

        when:
        service.setLastAccessedUrl(session, '/test/url')

        and:
        HttpSession httpSession = service.retrieveSession(servletContext, session.id)

        then:
        httpSession

        and:
        httpSession.getAttribute('emailAddress') == 'test@test.com'
        httpSession.getAttribute('lastUrl') == '/test/url'
    }

    void 'destroy session info'() {
        given:
        initialiseContext()
        service.setUserEmailAddress(session, 'test@test.com')
        service.setLastAccessedUrl(session, '/test/url')

        when:
        service.destroySession(session)

        and:
        HttpSession httpSession = service.retrieveSession(servletContext, session.id)

        then:
        !httpSession
    }

    private void initialiseContext() {
        try {
            // we have to wrap due to thrown exception from mock servlet
            service.initialiseToContext(servletContext)
        } catch (UnsupportedOperationException ignored) {
            //ignore
        }
    }
}
