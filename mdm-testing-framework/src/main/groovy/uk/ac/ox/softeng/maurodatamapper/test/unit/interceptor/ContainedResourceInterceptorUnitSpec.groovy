package uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

/**
 * @since 02/12/2019
 */
@Slf4j
abstract class ContainedResourceInterceptorUnitSpec<T> extends ResourceInterceptorUnitSpec<T> {

    abstract String getExpectedExceptionCodeForNoContainingItem()

    @Override
    void setResourceIdParameter(String id, String action) {
        setContainingResourceParameters(id)
        super.setResourceIdParameter(UUID.randomUUID().toString(), action)
    }

    abstract void setContainingResourceParameters(String id)

    @Unroll
    void 'CR1 : test read/write access to index is controlled for #type resource'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager

        when:
        setResourceIdParameter(resourceId.toString(), action)
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == allowed

        and:
        !responseCode || response.status == responseCode.code

        where:
        action  | resourceId    || allowed | responseCode
        'index' | unknownId     || false   | HttpStatus.UNAUTHORIZED
        'index' | noAccessId    || false   | HttpStatus.UNAUTHORIZED
        'index' | readAccessId  || true    | null
        'index' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }

    void 'CR2 : test exception thrown with no containing item'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager

        when:
        params.id = UUID.randomUUID().toString()
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == expectedExceptionCodeForNoContainingItem
    }
}
