package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.FolderInterceptor
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

class FolderInterceptorSpec extends ResourceInterceptorUnitSpec implements InterceptorUnitTest<FolderInterceptor> {

    @Override
    String getControllerName() {
        'folder'
    }

    @Override
    boolean getNoAccessIndexAllowedState() {
        true
    }

    @Override
    HttpStatus getNoAccessIndexAllowedCode() {
        HttpStatus.OK
    }

    @Unroll
    void 'test read/write access to #action is controlled for #type on nested folder'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager

        when:
        params.folderId = resourceId.toString()
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == allowed

        and:
        !responseCode || response.status == responseCode.code

        where:
        action   | resourceId    || allowed | responseCode
        'index'  | unknownId     || false   | HttpStatus.UNAUTHORIZED
        'index'  | noAccessId    || false   | HttpStatus.UNAUTHORIZED
        'index'  | readAccessId  || true    | null
        'index'  | writeAccessId || true    | null
        'show'   | unknownId     || false   | HttpStatus.UNAUTHORIZED
        'show'   | noAccessId    || false   | HttpStatus.UNAUTHORIZED
        'show'   | readAccessId  || true    | null
        'show'   | writeAccessId || true    | null
        'save'   | unknownId     || false   | HttpStatus.UNAUTHORIZED
        'save'   | noAccessId    || false   | HttpStatus.UNAUTHORIZED
        'save'   | readAccessId  || false   | HttpStatus.UNAUTHORIZED
        'save'   | writeAccessId || true    | null
        'update' | unknownId     || false   | HttpStatus.UNAUTHORIZED
        'update' | noAccessId    || false   | HttpStatus.UNAUTHORIZED
        'update' | readAccessId  || false   | HttpStatus.UNAUTHORIZED
        'update' | writeAccessId || true    | null
        'delete' | unknownId     || false   | HttpStatus.UNAUTHORIZED
        'delete' | noAccessId    || false   | HttpStatus.UNAUTHORIZED
        'delete' | readAccessId  || false   | HttpStatus.UNAUTHORIZED
        'delete' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }
}
