package uk.ac.ox.softeng.maurodatamapper.core.email

import uk.ac.ox.softeng.maurodatamapper.core.email.EmailInterceptor
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.SimpleInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Shared

class EmailInterceptorSpec extends SimpleInterceptorUnitSpec implements InterceptorUnitTest<EmailInterceptor> {

    @Shared
    List<String> actions = ['index']


    @Override
    String getControllerName() {
        'email'
    }
}
