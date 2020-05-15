package uk.ac.ox.softeng.maurodatamapper.core.interceptor

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

import groovy.util.logging.Slf4j

@Slf4j
abstract class SecurableResourceInterceptor implements MdmInterceptor {

    public static final String CLASS_PARAMS_KEY = 'securableResourceClass'
    public static final String ID_PARAMS_KEY = 'securableResourceId'

    abstract <S extends SecurableResource> Class<S> getSecuredClass()

    void setSecurableResourceParams() {
        if (!params.containsKey(CLASS_PARAMS_KEY)) params[CLASS_PARAMS_KEY] = getSecuredClass()
        params[ID_PARAMS_KEY] = getSecuredId()
    }

    abstract void checkIds()

    UUID getSecuredId() {
        getId()
    }

    abstract UUID getId()

    void securableResourceChecks() {
        checkIds()
        setSecurableResourceParams()
    }

    /**
     * Check the current action is allowed against the {@link SecurableResource} and id.
     *
     * @param securableResourceClass
     * @param id
     * @return
     */
    boolean checkActionAuthorisationOnSecuredResource(Class<? extends SecurableResource> securableResourceClass, UUID id,
                                                      boolean directCallsToIndexAllowed = false) {

        // Allows for direct calls to secured resources but stops indexing on nested secured resources
        if (directCallsToIndexAllowed && isIndex() && !id) return true

        // The 3 tiers of writing are deleting, updating/editing and saving/creation
        // We will have to handle certain controls inside the controller
        if (isDelete()) {
            return currentUserSecurityPolicyManager.userCanDeleteSecuredResourceId(
                securableResourceClass, id, params.boolean('permanent', false)) ?: unauthorised()
        }
        if (isUpdate()) {
            return currentUserSecurityPolicyManager.userCanEditSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }
        if (isSave()) {
            return currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }
        // If index or show then if user can read then they can see it
        if (isIndex() || isShow()) {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }

        unauthorised()
    }
}
