package uk.ac.ox.softeng.maurodatamapper.core.interceptor


import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

abstract class TieredAccessSecurableResourceInterceptor extends SecurableResourceInterceptor {

    List<String> getPublicAccessMethods() {
        []
    }

    List<String> getAuthenticatedAccessMethods() {
        []
    }

    List<String> getReadAccessMethods() {
        []
    }

    List<String> getCreateAccessMethods() {
        []
    }

    List<String> getEditAccessMethods() {
        []
    }

    List<String> getDeleteAccessMethods() {
        []
    }

    List<String> getApplicationAdminAccessMethods() {
        []
    }

    boolean checkTieredAccessActionAuthorisationOnSecuredResource(Class<? extends SecurableResource> securableResourceClass, UUID id,
                                                                  boolean directCallsToIndexAllowed = false) {

        if (actionName in getPublicAccessMethods()) {
            return true
        }

        if (actionName in getAuthenticatedAccessMethods()) {
            return currentUserSecurityPolicyManager.isLoggedIn() ?: unauthorised()
        }

        if (actionName in getReadAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }

        if (actionName in getCreateAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }

        if (actionName in getEditAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanEditSecuredResourceId(securableResourceClass, id) ?: unauthorised()
        }

        if (actionName in getDeleteAccessMethods()) {
            return currentUserSecurityPolicyManager.userCanDeleteSecuredResourceId(
                securableResourceClass, id, params.boolean('permanent', false)) ?: unauthorised()
        }

        if (actionName in getApplicationAdminAccessMethods()) {
            return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: unauthorised()
        }

        checkActionAuthorisationOnSecuredResource(securableResourceClass, id, directCallsToIndexAllowed)
    }
}
