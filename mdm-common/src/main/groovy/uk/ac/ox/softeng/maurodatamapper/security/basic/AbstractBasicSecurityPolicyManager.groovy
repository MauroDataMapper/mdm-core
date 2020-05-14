package uk.ac.ox.softeng.maurodatamapper.security.basic

import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

abstract class AbstractBasicSecurityPolicyManager implements UserSecurityPolicyManager {

    abstract boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action)

    abstract boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                            UUID owningSecureResourceId, String action)

    @Override
    boolean userCanCreateResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                    UUID owningSecureResourceId) {
        userCanWriteResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId, 'save')
    }

    @Override
    boolean userCanEditResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                  UUID owningSecureResourceId) {
        userCanWriteResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId, 'update')
    }

    @Override
    boolean userCanDeleteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                    UUID owningSecureResourceId) {
        userCanWriteResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId, 'softDelete')
    }

    @Override
    boolean userCanCreateSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        userCanWriteSecuredResourceId(securableResourceClass, id, 'save')
    }

    @Override
    boolean userCanEditSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        userCanWriteSecuredResourceId(securableResourceClass, id, 'update')
    }

    @Override
    boolean userCanDeleteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, boolean permanent) {
        userCanWriteSecuredResourceId(securableResourceClass, id, permanent ? 'delete' : 'softDelete')
    }

    @Override
    List<String> userAvailableActions(Class resourceClass, UUID id) {
        id ? ['delete', 'show', 'update'] : ['index', 'save']
    }

    @Override
    List<String> userAvailableActions(String domainType, UUID id) {
        id ? ['delete', 'show', 'update'] : ['index', 'save']
    }
}
