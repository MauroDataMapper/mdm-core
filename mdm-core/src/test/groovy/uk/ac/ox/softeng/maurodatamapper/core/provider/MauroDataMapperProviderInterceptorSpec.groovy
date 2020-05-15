package uk.ac.ox.softeng.maurodatamapper.core.provider


import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.SimpleInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Shared

class MauroDataMapperProviderInterceptorSpec extends SimpleInterceptorUnitSpec
    implements InterceptorUnitTest<MauroDataMapperProviderInterceptor> {

    @Shared
    List<String> actions = [
        'modules'
    ]

    @Override
    String getControllerName() {
        'mauroDataMapperProvider'
    }
}
