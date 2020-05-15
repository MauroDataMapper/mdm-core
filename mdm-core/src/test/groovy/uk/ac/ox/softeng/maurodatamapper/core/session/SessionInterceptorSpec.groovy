package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.core.session.SessionInterceptor
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Unroll

class SessionInterceptorSpec extends BaseUnitSpec implements InterceptorUnitTest<SessionInterceptor> {

    void "Test activeSession interceptor matching"() {
        when: "A request sent to the interceptor"
        withRequest(uri: '/api/dataModels')

        then: "The interceptor does match"
        interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/api/authentication/logout')

        then: "The interceptor does match"
        interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc/api/dataModels')

        then: "The interceptor does match"
        interceptor.doesMatch()
    }

    void "Test activeSession interceptor non-matching"() {
        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc/')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/views/home.html')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/scripts/services/jointDiagramService3.js')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/bower_components/angular-tree-control/css/tree-control.css')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc/styles/vendor.5a20e16f.css')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc/scripts/scripts.91b6fb8f.js')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }

    @Unroll
    void 'test controller exclusions: #controller:#action'() {

        when: "A request sent to the interceptor"
        withRequest(uri: "/api/$controller/${action == 'index' ? '' : action}")
        withRequest(controller: controller, action: action)

        then: "The interceptor does not match"
        interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: "/oxfordbrc/api/$controller/${action == 'index' ? '' : action}")
        withRequest(controller: controller, action: action)

        then: "The interceptor does not match"
        interceptor.doesMatch()

        where:
        controller       | action
        'authentication' | 'login'
        'authentication' | 'logout'
        'authentication' | 'isValidSession'
        'admin'          | 'apiVersion'
        'admin'          | 'coreVersion'
        'admin'          | 'activeSessions'
        'metadata'       | 'namespace'
        'catalogueUser'  | 'image'
        'catalogueUser'  | 'userExists'
        'catalogueUser'  | 'save'
        'classifier'     | 'index'
        'dataModel'      | 'index'
        'treeItem'       | 'index'
        'treeItem'       | 'search'
        'catalogueItem'  | 'search'
    }

    @Unroll
    void 'test uri "#uri"'() {
        when: "A request sent to the interceptor"
        withRequest(uri: "/api/$uri")

        then: "The interceptor does not match"
        interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: "/$uri")

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: "/oxfordbrc/api/$uri")

        then: "The interceptor does not match"
        interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: "/oxfordbrc/$uri")

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        where:
        uri << [
            'error',
            'notFound',
            'notImplemented',
            'shutdown'
        ]
    }
}
