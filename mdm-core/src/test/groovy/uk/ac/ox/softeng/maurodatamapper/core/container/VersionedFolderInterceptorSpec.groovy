package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

class VersionedFolderInterceptorSpec extends ResourceInterceptorUnitSpec implements InterceptorUnitTest<VersionedFolderInterceptor> {

    @Override
    String getControllerName() {
        'versionedFolder'
    }

    @Override
    boolean getNoAccessIndexAllowedState() {
        true
    }

    @Override
    HttpStatus getNoAccessIndexAllowedCode() {
        HttpStatus.OK
    }

    @Override
    HttpStatus getSaveAllowedCode() {
        HttpStatus.FORBIDDEN
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
        'index'  | unknownId     || false   | HttpStatus.NOT_FOUND
        'index'  | noAccessId    || false   | HttpStatus.NOT_FOUND
        'index'  | readAccessId  || true    | null
        'index'  | writeAccessId || true    | null
        'show'   | unknownId     || false   | HttpStatus.NOT_FOUND
        'show'   | noAccessId    || false   | HttpStatus.NOT_FOUND
        'show'   | readAccessId  || true    | null
        'show'   | writeAccessId || true    | null
        'save'   | unknownId     || false   | getSaveAllowedCode()
        'save'   | noAccessId    || false   | getSaveAllowedCode()
        'save'   | readAccessId  || false   | HttpStatus.FORBIDDEN
        'save'   | writeAccessId || true    | null
        'update' | unknownId     || false   | HttpStatus.NOT_FOUND
        'update' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'update' | readAccessId  || false   | HttpStatus.FORBIDDEN
        'update' | writeAccessId || true    | null
        'delete' | unknownId     || false   | HttpStatus.NOT_FOUND
        'delete' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'delete' | readAccessId  || false   | HttpStatus.FORBIDDEN
        'delete' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }

    @Unroll
    void 'test access to search is controlled for #type on folder'() {
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
        'search' | unknownId     || false   | HttpStatus.NOT_FOUND
        'search' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'search' | readAccessId  || true    | null
        'search' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }
}