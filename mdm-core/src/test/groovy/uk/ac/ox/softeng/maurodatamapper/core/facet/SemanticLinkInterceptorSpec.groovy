package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.VariableContainedResourceInterceptorSpec

import grails.testing.web.interceptor.InterceptorUnitTest

class SemanticLinkInterceptorSpec extends VariableContainedResourceInterceptorSpec implements InterceptorUnitTest<SemanticLinkInterceptor> {

    def setup() {
        mockDomains(BasicModel)
    }

    @Override
    String getControllerName() {
        'semanticLink'
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
}
