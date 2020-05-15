package uk.ac.ox.softeng.maurodatamapper.core.importer

import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterInterceptor
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.SimpleInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Shared

class ImporterInterceptorSpec extends SimpleInterceptorUnitSpec implements InterceptorUnitTest<ImporterInterceptor> {

    @Shared
    List<String> actions = ['parameters']

    @Override
    String getControllerName() {
        'importer'
    }
}
