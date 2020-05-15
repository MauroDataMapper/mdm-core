package uk.ac.ox.softeng.maurodatamapper.core.traits.controller

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsClass
import grails.web.api.WebAttributes
import grails.web.servlet.mvc.GrailsParameterMap

/**
 * @since 25/11/2019
 */

trait MdmInterceptor implements UserSecurityPolicyManagerAware, WebAttributes {

    GrailsClass getGrailsResource(GrailsParameterMap params, String resourceParam) {
        String lookup = params[resourceParam]

        if (!lookup) {
            throw new ApiBadRequestException('MCI01', "No domain class resource provided")
        }

        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, lookup)
        if (!grailsClass) {
            throw new ApiBadRequestException('MCI02', "Unrecognised domain class resource [${params[resourceParam]}]")
        }
        grailsClass
    }

    void mapDomainTypeToClass(String paramKey, boolean required = false) {
        String key = paramKey + 'DomainType'
        String classKey = paramKey + 'Class'
        String idKey = paramKey + 'Id'
        if (required || params.containsKey(key)) {
            Utils.toUuid(params, idKey)
            GrailsClass grailsClass = getGrailsResource(params, key)
            params[key] = grailsClass.shortName
            params[classKey] = grailsClass.clazz
        }
    }

    boolean isIndex() {
        actionName == 'index'
    }

    boolean isSave() {
        actionName == 'save'
    }

    boolean isDelete() {
        actionName == 'delete'
    }

    boolean isShow() {
        actionName == 'show'
    }

    boolean isUpdate() {
        actionName == 'update'
    }

    boolean isKnownAction() {
        isIndex() || isSave() || isDelete() || isShow() || isUpdate()
    }

    /**
     * Check the current action is allowed against the resource and id.
     * The resource may or may not be a {@link SecurableResource}. This method should be used for interceptors which cover domains which belong to
     * a {@link SecurableResource} or domains which belong to a domain which belongs to a {@link SecurableResource}.
     *
     * @param resourceClass
     * @param id
     * @return
     */
    boolean checkActionAuthorisationOnUnsecuredResource(Class resourceClass, UUID id,
                                                        Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        // The 3 tiers of writing are deleting, updating/editing and saving/creation
        // We will have to handle certain controls inside the controller
        if (isDelete()) {
            return currentUserSecurityPolicyManager.userCanDeleteResourceId(resourceClass, id,
                                                                            owningSecureResourceClass, owningSecureResourceId) ?: unauthorised()
        }
        if (isUpdate()) {
            return currentUserSecurityPolicyManager.userCanEditResourceId(resourceClass, id,
                                                                          owningSecureResourceClass, owningSecureResourceId) ?: unauthorised()
        }
        if (isSave()) {
            return currentUserSecurityPolicyManager.userCanCreateResourceId(resourceClass, id,
                                                                            owningSecureResourceClass, owningSecureResourceId) ?: unauthorised()
        }
        // If index or show then if user can read then they can see it
        if (isIndex() || isShow()) {
            return currentUserSecurityPolicyManager.userCanReadResourceId(resourceClass, id,
                                                                          owningSecureResourceClass, owningSecureResourceId) ?: unauthorised()
        }

        unauthorised()
    }
}