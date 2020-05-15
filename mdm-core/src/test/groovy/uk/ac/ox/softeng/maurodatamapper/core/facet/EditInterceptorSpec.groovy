package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.EditInterceptor
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes

class EditInterceptorSpec extends BaseUnitSpec implements InterceptorUnitTest<EditInterceptor> {

    def setup() {
        mockDomains(Folder, Classifier)
    }

    void 'Test interceptor matching'() {
        when: 'A request matches the interceptor'
        withRequest(controller: 'edit')

        then: 'The interceptor does match'
        interceptor.doesMatch()

    }

    void 'test exception thrown with no resource'() {
        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'MCI01'
    }

    void 'test execption thrown with unrecognised resource'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        params.resourceDomainType = 'dataModels'
        params.resourceDomainId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'MCI02'
    }

    void 'test public access to index is allowed'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        params.resourceDomainType = 'folders'
        params.resourceDomainId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')

        then:
        interceptor.before()
    }

    void 'test noaccess to index is not allowed'() {
        given:
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance
        params.resourceDomainType = 'folders'
        params.resourceDomainId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')

        then:
        !interceptor.before()

        and:
        response.status == HttpStatus.UNAUTHORIZED.code

    }
}
