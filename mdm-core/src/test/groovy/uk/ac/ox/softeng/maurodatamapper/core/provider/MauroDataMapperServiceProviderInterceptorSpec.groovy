package uk.ac.ox.softeng.maurodatamapper.core.provider


import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.SimpleInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Shared

class MauroDataMapperServiceProviderInterceptorSpec extends SimpleInterceptorUnitSpec
    implements InterceptorUnitTest<MauroDataMapperServiceProviderInterceptor> {
    @Shared
    List<String> actions = [
        'exporterProviders',
        'emailProviders',
        'dataLoaderProviders',
        'importerProviders'
    ]

    @Override
    String getControllerName() {
        'mauroDataMapperServiceProvider'
    }
}
