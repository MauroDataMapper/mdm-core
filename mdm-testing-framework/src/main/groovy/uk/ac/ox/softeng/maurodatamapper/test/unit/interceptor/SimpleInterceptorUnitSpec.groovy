package uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor


import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.artefact.Interceptor
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

/**
 * Any class extending this class needs to add:
 *
 * @Shared
 * List<String> actions = []
 *
 * @since 03/12/2019
 */
abstract class SimpleInterceptorUnitSpec extends BaseUnitSpec {

    abstract Interceptor getInterceptor()

    abstract String getControllerName()

    def getPublicAccessUserSecurityPolicyManager() {
        PublicAccessSecurityPolicyManager.instance
    }

    def getNoAccessUserSecurityPolicyManager() {
        NoAccessSecurityPolicyManager.instance
    }

    void 'S1 : Test interceptor matching'() {
        when: 'A request matches the interceptor'
        withRequest(controller: controllerName)

        then: 'The interceptor does match'
        interceptor.doesMatch()

    }

    @Unroll
    void 'S2 : test public access to #action is allowed'() {
        given:
        assert actions
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager

        when:
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()

        where:
        action << actions
    }

    @Unroll
    void 'S3 : test access to #action is not allowed'() {
        given:
        assert actions
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager

        when:
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()

        and:
        response.status == HttpStatus.UNAUTHORIZED.code

        where:
        action << actions
    }
}
