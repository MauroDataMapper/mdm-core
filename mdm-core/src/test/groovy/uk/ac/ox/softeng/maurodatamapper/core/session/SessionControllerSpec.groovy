package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.core.session.SessionController
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.controllers.ControllerUnitTest

import static org.springframework.http.HttpStatus.OK

class SessionControllerSpec extends BaseUnitSpec implements ControllerUnitTest<SessionController> {

    void 'test isAuthenticatedSession when its not valid'() {
        given:
        controller.sessionService = Mock(SessionService) {
            1 * isAuthenticatedSession(_) >> false
        }

        when:
        request.method = 'GET'
        controller.isAuthenticatedSession()

        then:
        response.status == OK.value()
        response.json.authenticatedSession == false
    }

    void 'test isAuthenticatedSession'() {
        given:
        controller.sessionService = Mock(SessionService) {
            1 * isAuthenticatedSession(_) >> true
        }
        when:
        request.method = 'GET'
        controller.isAuthenticatedSession()

        then:
        response.status == OK.value()
        response.json.authenticatedSession == true
    }
}
