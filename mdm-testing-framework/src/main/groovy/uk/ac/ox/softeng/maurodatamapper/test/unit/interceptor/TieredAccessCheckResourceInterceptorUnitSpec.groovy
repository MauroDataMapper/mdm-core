package uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.TieredAccessSecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.IdSecuredUserSecurityPolicyManager

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

@Slf4j
abstract class TieredAccessCheckResourceInterceptorUnitSpec<T extends TieredAccessSecurableResourceInterceptor>
    extends ResourceInterceptorUnitSpec<T> {

    void setParamsId(UUID id) {
        params.id = id
    }

    @Unroll
    void 'test access to #action using #idStr is always available'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        where:
        [action, id] <<
        [interceptor.getPublicAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }

    @Unroll
    void 'test access to #action using #userStr is only available to authenticated users'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(adminId)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(adminId)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.UNAUTHORIZED.code

        when:
        response.reset()
        setParamsId(adminId)
        params.currentUserSecurityPolicyManager =
            new IdSecuredUserSecurityPolicyManager(user, unknownId, noAccessId, readAccessId, writeAccessId)
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        setParamsId(adminId)
        interceptor.before() == (UnloggedUser.instance.emailAddress != user.emailAddress && user.emailAddress != 'pending@test.com')
        response.status == (UnloggedUser.instance.emailAddress != user.emailAddress
                                && user.emailAddress != 'pending@test.com' ? HttpStatus.OK.code : HttpStatus.UNAUTHORIZED.code)

        where:
        [action, user] << [interceptor.getAuthenticatedAccessMethods() ?: ['no-endpoints'],
                           [UnloggedUser.instance,
                            admin, editor, pending, reader1, reader2
                           ]].combinations()
        userStr = user.emailAddress
    }

    @Unroll
    void 'test access to #action using #idStr is only available to readers'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.UNAUTHORIZED.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id in [writeAccessId, readAccessId])
        response.status == (id in [writeAccessId, readAccessId] ? HttpStatus.OK.code : HttpStatus.UNAUTHORIZED.code)

        where:
        [action, id] << [interceptor.getReadAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }


    @Unroll
    void 'test access to #action using #idStr is only available to creators'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.UNAUTHORIZED.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id == writeAccessId)
        response.status == (id == writeAccessId ? HttpStatus.OK.code : HttpStatus.UNAUTHORIZED.code)

        where:
        [action, id] <<
        [interceptor.getCreateAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }

    @Unroll
    void 'test access to #action using #idStr is only available to editors'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.UNAUTHORIZED.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id == writeAccessId)
        response.status == (id == writeAccessId ? HttpStatus.OK.code : HttpStatus.UNAUTHORIZED.code)

        where:
        [action, id] << [interceptor.getEditAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }

    @Unroll
    void 'test access to #action using #idStr is only available to deleters'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.UNAUTHORIZED.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id == writeAccessId)
        response.status == (id == writeAccessId ? HttpStatus.OK.code : HttpStatus.UNAUTHORIZED.code)

        where:
        [action, id] <<
        [interceptor.getDeleteAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }

    @Unroll
    void 'test access to #action using #idStr is only available to admins'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.UNAUTHORIZED.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.UNAUTHORIZED.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = applicationAdminSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        where:
        [action, id] <<
        [interceptor.getApplicationAdminAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }

}
