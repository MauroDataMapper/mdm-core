package uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager

import groovy.util.logging.Slf4j
import org.grails.web.util.GrailsApplicationAttributes

/**
 * @since 20/03/2020
 */
@Slf4j
abstract class VariableContainedResourceInterceptorSpec<T> extends ContainedResourceInterceptorUnitSpec<T> {

    abstract void setUnknownContainingItemDomainType()

    @Override
    String getExpectedExceptionCodeForNoContainingItem() {
        'MCI01'
    }

    void 'VCR1 : test execption thrown with unrecognised containing item'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        setResourceIdParameter(UUID.randomUUID().toString(), '')
        setUnknownContainingItemDomainType()

        when:
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'MCI02'
    }
}
