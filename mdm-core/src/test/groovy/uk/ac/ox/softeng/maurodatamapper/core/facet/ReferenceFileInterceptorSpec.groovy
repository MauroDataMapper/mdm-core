package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.VariableContainedResourceInterceptorSpec

import grails.testing.web.interceptor.InterceptorUnitTest

class ReferenceFileInterceptorSpec extends VariableContainedResourceInterceptorSpec implements InterceptorUnitTest<ReferenceFileInterceptor> {

    def setup() {
        mockDomains(BasicModel)
    }

    @Override
    String getControllerName() {
        'referenceFile'
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
