package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.VariableContainedResourceInterceptorSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.web.util.GrailsApplicationAttributes

class MetadataInterceptorSpec extends VariableContainedResourceInterceptorSpec implements InterceptorUnitTest<MetadataInterceptor> {

    def setup() {
        mockDomains(BasicModel)
    }

    @Override
    String getControllerName() {
        'metadata'
    }

    @Override
    void setContainingResourceParameters(String id) {
        params.catalogueItemDomainType = 'basicModels'
        params.catalogueItemId = id
    }

    @Override
    void setAnyInitialParams() {
        params.catalogueItemDomainType = 'basicModels'
        params.catalogueItemId = UUID.randomUUID().toString()
    }

    @Override
    void setUnknownContainingItemDomainType() {
        params.catalogueItemDomainType = 'dataModels'
    }

    void 'test public access to namespaces is allowed'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance

        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'namespaces')

        then:
        interceptor.before()
    }

    void 'test noaccess to namespaces is allowed'() {
        given:
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance

        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'namespaces')

        then:
        interceptor.before()

    }
}
