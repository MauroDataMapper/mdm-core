package uk.ac.ox.softeng.maurodatamapper.core.admin


import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.SimpleInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Shared

class AdminInterceptorSpec extends SimpleInterceptorUnitSpec implements InterceptorUnitTest<AdminInterceptor> {

    @Shared
    List<String> actions = [
        'status',
        'editApiProperties',
        'rebuildLuceneIndexes',
        'apiProperties'
    ]

    @Override
    String getControllerName() {
        'admin'
    }
}
