package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFileInterceptor

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class UserImageFileInterceptorSpec extends Specification implements InterceptorUnitTest<UserImageFileInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test userImageFile interceptor matching"() {
        when: "A request matches the interceptor"
        withRequest(controller: "userImageFile")

        then: "The interceptor does match"
        interceptor.doesMatch()
    }
}
