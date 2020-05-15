package uk.ac.ox.softeng.maurodatamapper.core.tree

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes

class TreeItemInterceptorSpec extends BaseUnitSpec implements InterceptorUnitTest<TreeItemInterceptor> {

    def setup() {
        mockDomains(Folder, Classifier, BasicModel)
    }

    void 'Test interceptor matching'() {
        when: 'A request matches the interceptor'
        withRequest(controller: 'treeItem')

        then: 'The interceptor does match'
        interceptor.doesMatch()

    }

    void 'test exception thrown with no resource'() {
        when:
        withRequest([controller: 'treeItem'])
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
        params.containerDomainType = 'dataModels'

        when:
        withRequest([controller: 'treeItem'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'MCI02'
    }

    void 'test public access to full tree is allowed'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        params.containerDomainType = 'folders'

        when:
        withRequest([controller: 'treeItem'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')

        then:
        interceptor.before()
    }

    void 'test noaccess to full tree is allowed'() {
        given:
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance
        params.containerDomainType = 'folders'

        when:
        withRequest([controller: 'treeItem'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')

        then:
        interceptor.before()
    }

    void 'test public access to id index is allowed'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        params.containerDomainType = 'folders'
        params.catalogueItemDomainType = 'basicModels'
        params.catalogueItemId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'treeItem'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'show')

        then:
        interceptor.before()
    }

    void 'test noaccess to id index is not allowed'() {
        given:
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance
        params.containerDomainType = 'folders'
        params.catalogueItemDomainType = 'basicModels'
        params.catalogueItemId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'treeItem'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'show')

        then:
        !interceptor.before()

        and:
        response.status == HttpStatus.UNAUTHORIZED.code

    }
}
