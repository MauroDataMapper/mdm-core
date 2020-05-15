package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkInterceptor
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.VariableContainedResourceInterceptorSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.web.util.GrailsApplicationAttributes

class VersionLinkInterceptorSpec extends VariableContainedResourceInterceptorSpec implements InterceptorUnitTest<VersionLinkInterceptor> {

    def setup() {
        mockDomains(BasicModel, BasicModelItem)
    }

    @Override
    String getControllerName() {
        'versionLink'
    }

    @Override
    void setContainingResourceParameters(String id) {
        params.modelDomainType = 'basicModels'
        params.modelId = id
    }

    @Override
    void setAnyInitialParams() {
        params.modelDomainType = 'basicModels'
        params.modelId = UUID.randomUUID().toString()
    }

    @Override
    void setUnknownContainingItemDomainType() {
        params.modelDomainType = 'dataModels'
    }

    void 'test exception thrown with no model'() {
        when:
        withRequest([controller: 'versionLink'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'MCI01'
    }

    void 'test execption thrown with non-model domain type'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        params.modelDomainType = 'basicModelItems'
        params.modelId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'versionLink'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'VLI01'
    }
}
